/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.mvc.model.trace

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.camel.IkoConstants.Headers.ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER
import com.ritense.iko.camel.IkoConstants.Properties.RELATION_RELATION_PROPERTY_NAME_PROPERTY
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_INSTANCE_TAG_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_OPERATION_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_TAG_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_VERSION_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.DEBUG_HTTP_RESPONSE_BODY_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.DEBUG_HTTP_RESPONSE_CODE_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.DEBUG_HTTP_RESPONSE_HEADERS_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.DEBUG_HTTP_RESPONSE_TEXT_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.DEBUG_PRE_AGGREGATION_JSON_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.DEBUG_PRE_RESULT_TRANSFORM_JSON_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.ENDPOINT_TRANSFORM_CONTEXT_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.ENDPOINT_TRANSFORM_RESULT_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.PROFILE_NAME
import com.ritense.iko.camel.IkoConstants.Variables.PROFILE_VERSION
import com.ritense.iko.camel.OutgoingHttpCapture
import org.apache.camel.spi.BacklogTracerEventMessage
import org.springframework.stereotype.Component

/**
 * Turns the raw list of [BacklogTracerEventMessage]s captured for a single debug run into a curated
 * [FlowTraceGraph] (git-branch nodes + links) plus a `stepId -> `[FlowTraceStep] detail map for the modal.
 *
 * **Node-level curation.** Unlike a route-entry view, we select the *specific processor nodes* that carry
 * business meaning, identified by (route id + node short-name + node label). This is grounded in a real
 * captured run (see `~/Projects/claude/research/2026-08-06-issue-314-adp-trace-node-map/`):
 * - `adp:<name>:<ver>:route-root`         `from`=[ADP entry], `enrich`=[aggregation],
 *                                          `transform`=[ADP result transform], `marshal`=[ADP result]
 * - `adp:<name>:<ver>:endpoint-transform` `setVariable[endpointTransformResult]`=[ADP endpoint transform]
 * - `connector:<tag>:...`(operation)      `from`=[HTTP operation] (response paired via `CamelHttpResponseCode`)
 * - `connector:<tag>:...*transform*`       `from`=[connector transform] (only when the connector ships one)
 * - `relation:<prop>:root`                `from`=[relation start], `setVariable[endpointTransformResult]`=[source mapping]
 * - `relation:<prop>:map|array`           `transform`=[relation result transform], `unmarshal`=[relation result]
 *
 * The tracer captures the body *entering* each node, so a step's [FlowTraceStep.output] is the body of the
 * next event on the same exchange (for an HTTP operation, the paired response). JQ expressions are config,
 * not trace, so they are resolved from the passed [AggregatedDataProfile]. The BacklogTracer does **not**
 * expose a resolved outgoing URL/protocol (those accessors are null / `isRemoteEndpoint` is noise), so the
 * HTTP step carries only method + status; the connector tag/instance/operation identify the target.
 *
 * Branches are exchanges; relations run on child exchanges reached through an intermediate `:multicast`
 * shell exchange, which carries no business node and is folded out so a relation attaches to its real
 * parent (trunk or enclosing relation).
 */
@Component
internal class FlowTraceGraphBuilder(
    private val parser: FlowTraceEventParser,
    private val headerRedactor: HeaderRedactor,
) {

    fun build(
        messages: List<BacklogTracerEventMessage>,
        adp: AggregatedDataProfile? = null,
        outgoing: List<OutgoingHttpCapture> = emptyList(),
    ): FlowTraceGraph {
        if (messages.isEmpty()) {
            return FlowTraceGraph(emptyList(), emptyList(), emptyMap())
        }

        val parsed = messages.sortedBy { it.uid }.map { parser.parse(it) }
        val relationsByProperty = adp?.relations?.associateBy { it.propertyName } ?: emptyMap()
        val outgoingBySpan = outgoing.filter { it.spanId != null }.associateBy { it.spanId!! }

        val exchangeParent = HashMap<String, String>()
        parsed.forEach { p ->
            if (p.correlationExchangeId != null) exchangeParent.putIfAbsent(p.exchangeId, p.correlationExchangeId)
        }

        val selected = parsed.mapNotNull { p -> stepTypeOf(p)?.let { it to p } }
            .groupBy { (type, p) -> Triple(type, p.routeId, p.exchangeId) }
            .map { (_, evts) -> evts.first().first to evts.minByOrNull { it.second.uid }!!.second }

        val businessExchanges = selected.map { it.second.exchangeId }.toHashSet()
        fun effectiveParent(exchangeId: String): String? {
            var current = exchangeParent[exchangeId]
            val seen = HashSet<String>()
            while (current != null && seen.add(current)) {
                if (current in businessExchanges) return current
                current = exchangeParent[current]
            }
            return null
        }

        val relationPropByExchange = HashMap<String, String>()
        parsed.forEach { p ->
            p.variables[RELATION_RELATION_PROPERTY_NAME_PROPERTY]?.let {
                relationPropByExchange.putIfAbsent(p.exchangeId, it)
            }
        }
        val minUidByExchange = selected.groupBy { it.second.exchangeId }
            .mapValues { (_, s) -> s.minOf { it.second.uid } }
        val iterationCountByExchange = HashMap<String, Int>()
        val collapsedExchanges = HashSet<String>()
        selected.map { it.second.exchangeId }.distinct()
            .filter { relationPropByExchange[it] != null }
            .groupBy { (effectiveParent(it) ?: "") to relationPropByExchange[it] }
            .values.forEach { group ->
                if (group.size <= 1) return@forEach
                val representative = group.minByOrNull { minUidByExchange[it] ?: Long.MAX_VALUE }!!
                iterationCountByExchange[representative] = group.size
                group.forEach { if (it != representative) collapsedExchanges.add(it) }
            }
        fun isCollapsed(exchangeId: String): Boolean {
            var current: String? = exchangeId
            val seen = HashSet<String>()
            while (current != null && seen.add(current)) {
                if (current in collapsedExchanges) return true
                current = exchangeParent[current]
            }
            return false
        }

        fun chainReaches(from: String, target: String): Boolean {
            var current = effectiveParent(from)
            val seen = HashSet<String>()
            while (current != null && seen.add(current)) {
                if (current == target) return true
                current = effectiveParent(current)
            }
            return false
        }

        val unordered = selected
            .filter { !isCollapsed(it.second.exchangeId) }
            .map { (type, rep) -> makeStep(type, rep, parsed, adp, relationsByProperty, outgoingBySpan) }

        fun orderKey(step: Step): Double {
            if (step.type != FlowTraceStepType.AGGREGATION) return step.rep.uid.toDouble()
            val x = step.rep.exchangeId
            val childMax = unordered
                .filter { it.rep.exchangeId != x && chainReaches(it.rep.exchangeId, x) }
                .maxOfOrNull { it.rep.uid }
            return childMax?.let { it + 0.5 } ?: step.rep.uid.toDouble()
        }
        val steps = unordered.sortedBy { orderKey(it) }

        // An array/loop relation's iteration child (its own exchange, no RELATION_START) folds onto its
        // relation-root lane so its per-element steps render inline within that relation instead of as a
        // separate lane.
        val relationRootExchanges = steps.filter { it.type == FlowTraceStepType.RELATION_START }.map { it.rep.exchangeId }.toHashSet()
        fun foldKey(exchangeId: String): String {
            if (exchangeId in relationRootExchanges) return exchangeId
            val parent = effectiveParent(exchangeId) ?: return exchangeId
            return if (parent in relationRootExchanges) parent else exchangeId
        }

        val nodes = ArrayList<FlowTraceNode>()
        val stepDetails = LinkedHashMap<String, FlowTraceStep>()
        steps.forEach { step ->
            nodes.add(
                FlowTraceNode(
                    name = step.name,
                    label = step.label,
                    category = step.category,
                    type = step.type,
                    status = step.status,
                    stepId = step.stepId,
                    branch = foldKey(step.rep.exchangeId),
                    entity = step.entity,
                    subroute = step.subroute,
                    iterations = iterationCountByExchange[step.rep.exchangeId],
                ),
            )
            stepDetails[step.stepId] = step.detail
        }

        val entryStep = steps.firstOrNull { it.type == FlowTraceStepType.ADP_ENTRY }
        val resultStep = steps.firstOrNull { it.type == FlowTraceStepType.ADP_RESULT }
        if (entryStep != null && resultStep != null) {
            val resultPayload = resultStep.detail.output ?: resultStep.detail.input
            stepDetails[entryStep.stepId] = stepDetails.getValue(entryStep.stepId).copy(output = resultPayload)
        }

        // Relation header output = the relation result, preferring the JSON result-transform output over the
        // unmarshalled object (which the tracer captures as a lossy Map.toString()).
        steps.filter { it.type == FlowTraceStepType.RELATION_START }.forEach { start ->
            val scope = steps.filter { it.rep.exchangeId == start.rep.exchangeId }
            val transform = scope.firstOrNull { it.type == FlowTraceStepType.RELATION_RESULT_TRANSFORM }
            val relResult = scope.firstOrNull { it.type == FlowTraceStepType.RELATION_RESULT }
            val payload = transform?.detail?.output
                ?: relResult?.detail?.input
                ?: relResult?.detail?.output
                ?: transform?.detail?.input
            if (payload != null) {
                stepDetails[start.stepId] = stepDetails.getValue(start.stepId).copy(output = payload)
            }
        }

        val links = buildLinks(steps, ::effectiveParent, ::foldKey)

        return FlowTraceGraph(nodes = nodes, links = links, steps = stepDetails)
    }

    /**
     * Chains the curated steps into a git-branch topology (branch = exchange). Within one exchange every
     * step is chained in `uid` order; a relation exchange **splits off the most-recent HTTP operation of
     * its effective parent** and **merges into the nearest ancestor's aggregation node** (falling back to
     * the trunk aggregation / ADP result). Each nesting level has its own aggregation (a relation's `enrich`
     * on its `relation:<prop>:*` route), so a nested relation merges into its parent relation, which in turn
     * merges into the trunk — rather than every branch merging into a single ADP-level aggregation. Links
     * are DAG-filtered (drop self-loops, duplicates and cycle-closing edges).
     */
    private fun buildLinks(steps: List<Step>, effectiveParent: (String) -> String?, foldKey: (String) -> String): List<FlowTraceLink> {
        val result = steps.firstOrNull { it.type == FlowTraceStepType.ADP_RESULT }
        val aggregationByExchange = steps.filter { it.type == FlowTraceStepType.AGGREGATION }.associateBy { it.rep.exchangeId }
        val trunkExchange = result?.rep?.exchangeId
            ?: steps.firstOrNull { it.type == FlowTraceStepType.ADP_ENTRY }?.rep?.exchangeId
        val trunkAggregation = trunkExchange?.let { aggregationByExchange[it] }

        fun mergeTargetFor(exchangeId: String): Step? {
            var current = effectiveParent(exchangeId)
            val seen = HashSet<String>()
            while (current != null && seen.add(current)) {
                aggregationByExchange[current]?.let { return it }
                current = effectiveParent(current)
            }
            return trunkAggregation ?: result
        }

        val byExchange = steps.groupBy { foldKey(it.rep.exchangeId) }
        val splitPointsByExchange = steps
            .filter { it.type == FlowTraceStepType.HTTP || it.type == FlowTraceStepType.CONNECTOR_OPERATION }
            .groupBy { foldKey(it.rep.exchangeId) }

        val candidates = ArrayList<Pair<Step, Step>>()
        byExchange.forEach { (exchangeId, evts) ->
            val chain = evts.sortedBy { it.rep.uid }
            for (i in 0 until chain.size - 1) {
                candidates.add(chain[i] to chain[i + 1])
            }
            val parent = effectiveParent(exchangeId)
            if (parent != null) {
                val first = chain.first()
                val splitPoint = splitPointsByExchange[parent]?.filter { it.rep.uid < first.rep.uid }?.maxByOrNull { it.rep.uid }
                    ?: splitPointsByExchange[parent]?.minByOrNull { it.rep.uid }
                    ?: byExchange[parent]?.minByOrNull { it.rep.uid }
                splitPoint?.let { candidates.add(it to first) }
            }
            val mergeTarget = mergeTargetFor(exchangeId)
            if (mergeTarget != null && exchangeId != mergeTarget.rep.exchangeId) {
                candidates.add(chain.last() to mergeTarget)
            }
        }

        val adjacency = HashMap<String, MutableSet<String>>()
        val added = HashSet<Pair<String, String>>()
        val links = ArrayList<FlowTraceLink>()
        candidates.forEach { (from, to) ->
            val edge = from.name to to.name
            if (from.name == to.name || edge in added) return@forEach
            if (canReach(adjacency, to.name, from.name)) return@forEach
            adjacency.getOrPut(from.name) { HashSet() }.add(to.name)
            added.add(edge)
            val failed = from.status == FlowTraceStatus.FAILED || to.status == FlowTraceStatus.FAILED
            links.add(
                FlowTraceLink(
                    source = from.name,
                    target = to.name,
                    status = if (failed) FlowTraceStatus.FAILED else FlowTraceStatus.OK,
                    stepId = to.stepId,
                ),
            )
        }
        return links
    }

    private fun canReach(adjacency: Map<String, Set<String>>, start: String, target: String): Boolean {
        val stack = ArrayDeque<String>().apply { add(start) }
        val visited = HashSet<String>()
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (node == target) return true
            if (!visited.add(node)) continue
            adjacency[node]?.forEach { stack.add(it) }
        }
        return false
    }

    private fun makeStep(
        type: FlowTraceStepType,
        rep: FlowTraceEvent,
        all: List<FlowTraceEvent>,
        adp: AggregatedDataProfile?,
        relationsByProperty: Map<String, com.ritense.iko.aggregateddataprofile.domain.Relation>,
        outgoingBySpan: Map<String, OutgoingHttpCapture>,
    ): Step {
        val scope = all.filter { it.exchangeId == rep.exchangeId && it.routeId == rep.routeId }
        // Attribute failure to the step's own event: a nested exception propagates up through ancestor
        // unwind `from`s on the same route+exchange, so `scope.any { it.failed }` would redden ancestors.
        val failed = rep.failed
        val status = when {
            type == FlowTraceStepType.ADP_ENTRY || type == FlowTraceStepType.RELATION_START -> FlowTraceStatus.OK
            failed -> FlowTraceStatus.FAILED
            scope.all { it.done } -> FlowTraceStatus.OK
            else -> FlowTraceStatus.IN_PROGRESS
        }
        val exception = if (failed) {
            scope.filter { it.uid >= rep.uid }.firstNotNullOfOrNull { it.exception }
                ?: scope.firstNotNullOfOrNull { it.exception }
        } else {
            null
        }
        val fromNode = rep.toNodeShortName == "from"
        val elapsed = if (fromNode) scope.maxOf { it.elapsed } else rep.elapsed

        val relProp = rep.routeId?.let { RELATION_ROUTE.find(it)?.groupValues?.getOrNull(1) }
        val relation = relProp?.let { relationsByProperty[it] }

        fun exchangeVar(name: String): String? = all.filter { it.exchangeId == rep.exchangeId }.firstNotNullOfOrNull { it.variables[name] }

        val responseEvt = if (type == FlowTraceStepType.HTTP) resolveResponseEvent(rep, all) else null
        val http = if (type == FlowTraceStepType.HTTP) {
            FlowTraceHttpInfo(
                method = responseEvt?.headers?.get(HTTP_METHOD_HEADER)
                    ?: all.filter { it.exchangeId == rep.exchangeId }
                        .firstNotNullOfOrNull { it.headers[HTTP_METHOD_HEADER] },
                status = responseEvt?.httpStatus
                    ?: exchangeVar(DEBUG_HTTP_RESPONSE_CODE_VARIABLE)?.toIntOrNull()
                    ?: statusFromException(exception),
                statusText = responseEvt?.headers?.get(HTTP_RESPONSE_TEXT_HEADER)
                    ?: exchangeVar(DEBUG_HTTP_RESPONSE_TEXT_VARIABLE),
            )
        } else {
            null
        }

        // Prefer the JSON snapshot captured during a debug run over the tracer's lossy Map.toString(). Read
        // pre-aggregation from the step's own event: it is captured before the enrich, so child exchanges
        // inherit the parent's value and a whole-exchange scan would pick the wrong one.
        val debugInputJson = when (type) {
            FlowTraceStepType.AGGREGATION -> rep.variables[DEBUG_PRE_AGGREGATION_JSON_VARIABLE]

            FlowTraceStepType.ADP_RESULT_TRANSFORM, FlowTraceStepType.RELATION_RESULT_TRANSFORM ->
                rep.variables[DEBUG_PRE_RESULT_TRANSFORM_JSON_VARIABLE]

            else -> null
        }
        val debugOutputJson = if (type == FlowTraceStepType.AGGREGATION) {
            all.filter { it.exchangeId == rep.exchangeId }
                .firstNotNullOfOrNull { it.variables[DEBUG_PRE_RESULT_TRANSFORM_JSON_VARIABLE] }
        } else {
            null
        }

        val inputBody = debugInputJson ?: rep.body
        val inputTruncated = debugInputJson == null && rep.truncated
        val input = if (type == FlowTraceStepType.RELATION_SOURCE_MAPPING && rep.headers.isEmpty()) {
            val entry = all.firstOrNull {
                it.exchangeId == rep.exchangeId && it.routeId == rep.routeId && it.toNodeShortName == "from"
            }
            FlowTraceMessagePayload(inputBody, headerRedactor.redact(entry?.headers ?: rep.headers), inputTruncated)
        } else {
            FlowTraceMessagePayload(inputBody, headerRedactor.redact(rep.headers), inputTruncated)
        }
        val output = when (type) {
            FlowTraceStepType.ADP_ENTRY, FlowTraceStepType.RELATION_START -> null

            else -> {
                val next = all.firstOrNull { it.exchangeId == rep.exchangeId && it.uid > rep.uid }
                when {
                    debugOutputJson != null -> FlowTraceMessagePayload(debugOutputJson, headerRedactor.redact(next?.headers ?: emptyMap()), false)
                    next != null -> FlowTraceMessagePayload(next.body, headerRedactor.redact(next.headers), next.truncated)
                    else -> null
                }
            }
        }

        val httpResponse = if (type == FlowTraceStepType.HTTP) {
            exchangeVar(DEBUG_HTTP_RESPONSE_BODY_VARIABLE)?.let {
                FlowTraceMessagePayload(it, headerRedactor.redact(parseHeaderLines(exchangeVar(DEBUG_HTTP_RESPONSE_HEADERS_VARIABLE))), false)
            } ?: responseEvt?.let { FlowTraceMessagePayload(it.body, headerRedactor.redact(it.headers), it.truncated) }
        } else {
            null
        }

        val mappedResult = if (type == FlowTraceStepType.ADP_ENDPOINT_TRANSFORM || type == FlowTraceStepType.RELATION_SOURCE_MAPPING) {
            exchangeVar(ENDPOINT_TRANSFORM_RESULT_VARIABLE)
        } else {
            null
        }

        val outgoing = if (type == FlowTraceStepType.HTTP) outgoingBySpan[rep.exchangeId] else null

        val stepId = "step-${rep.uid}"
        val detail = FlowTraceStep(
            stepId = stepId,
            type = type,
            label = type.label,
            status = status,
            elapsedMs = elapsed,
            branch = rep.exchangeId,
            fields = fieldsFor(type, rep, adp, relation, http, httpResponse, mappedResult, outgoing),
            input = input,
            output = output,
            http = http,
            exception = exception,
        )
        return Step(
            rep = rep,
            name = "n${rep.uid}",
            stepId = stepId,
            type = type,
            label = type.label,
            category = type.category,
            status = status,
            elapsed = elapsed,
            entity = entityFor(type, rep, relProp),
            subroute = type.subroute,
            detail = detail,
        )
    }

    /** The response of a connector call is a later same-exchange, same-route event carrying the
     *  `CamelHttpResponseCode` header (the snapshot right after the `toD`). Prefer the **earliest**
     *  status-bearing event that still carries a response body — later nodes in the connector route
     *  (unmarshal/unwind `from`) can clear or replace the body, so `maxByOrNull` would surface an empty
     *  body for some connectors. Fall back to the earliest status event, then the last event. */
    private fun resolveResponseEvent(from: FlowTraceEvent, all: List<FlowTraceEvent>): FlowTraceEvent? {
        val sameRoute = all.filter {
            it.exchangeId == from.exchangeId && it.routeId == from.routeId && it.uid > from.uid
        }
        if (sameRoute.isEmpty()) return null
        val withStatus = sameRoute.filter { it.headers.containsKey(HTTP_RESPONSE_CODE_HEADER) }
        return withStatus.sortedBy { it.uid }.firstOrNull { !it.body.isNullOrEmpty() }
            ?: withStatus.minByOrNull { it.uid }
    }

    private fun stepTypeOf(p: FlowTraceEvent): FlowTraceStepType? {
        val r = p.routeId ?: return null
        val short = p.toNodeShortName
        val label = p.toNodeLabel ?: ""
        return when {
            ADP_ROOT_ROUTE.matches(r) -> when (short) {
                "from" -> FlowTraceStepType.ADP_ENTRY
                "enrich" -> FlowTraceStepType.AGGREGATION
                "transform" -> FlowTraceStepType.ADP_RESULT_TRANSFORM
                "marshal" -> FlowTraceStepType.ADP_RESULT
                else -> null
            }

            ADP_ENDPOINT_TRANSFORM_ROUTE.matches(r) ->
                if (short == "setVariable" && label.contains(ENDPOINT_TRANSFORM_RESULT_MARKER)) {
                    FlowTraceStepType.ADP_ENDPOINT_TRANSFORM
                } else {
                    null
                }

            r.startsWith(CONNECTOR_ROUTE_PREFIX) -> when {
                short == "toD" && label.contains(REST_OPENAPI_MARKER, ignoreCase = true) -> FlowTraceStepType.HTTP
                short != "from" -> null
                r.contains(CONNECTOR_TRANSFORM_MARKER, ignoreCase = true) -> FlowTraceStepType.CONNECTOR_TRANSFORM
                else -> FlowTraceStepType.CONNECTOR_OPERATION
            }

            RELATION_ROUTE.matches(r) && short == "enrich" -> FlowTraceStepType.AGGREGATION

            RELATION_ROOT_ROUTE.matches(r) -> when {
                short == "from" -> FlowTraceStepType.RELATION_START

                short == "setVariable" && label.contains(ENDPOINT_TRANSFORM_RESULT_MARKER) ->
                    FlowTraceStepType.RELATION_SOURCE_MAPPING

                else -> null
            }

            RELATION_MAP_ARRAY_ROUTE.matches(r) -> when (short) {
                "transform" -> FlowTraceStepType.RELATION_RESULT_TRANSFORM
                "unmarshal" -> FlowTraceStepType.RELATION_RESULT
                else -> null
            }

            else -> null
        }
    }

    private fun entityFor(type: FlowTraceStepType, rep: FlowTraceEvent, relProp: String?): String? = when (type) {
        FlowTraceStepType.CONNECTOR_OPERATION, FlowTraceStepType.CONNECTOR_TRANSFORM, FlowTraceStepType.HTTP ->
            rep.variables[CONNECTOR_TAG_VARIABLE] ?: rep.variables[CONNECTOR_INSTANCE_TAG_VARIABLE]

        FlowTraceStepType.RELATION_START, FlowTraceStepType.RELATION_SOURCE_MAPPING,
        FlowTraceStepType.RELATION_RESULT_TRANSFORM, FlowTraceStepType.RELATION_RESULT,
        -> relProp

        FlowTraceStepType.AGGREGATION -> relProp ?: rep.variables[PROFILE_NAME]

        else -> rep.variables[PROFILE_NAME]
    }

    /**
     * The per-type detail fields shown in the modal. [mappedResult] is the `endpointTransformResult`
     * variable value (the parameters a JQ mapping produced) for the endpoint-transform / source-mapping
     * steps; the exchange itself does not carry it as body/headers at the mapping node, so it is surfaced
     * here as a `Result` code field.
     */
    private fun fieldsFor(
        type: FlowTraceStepType,
        rep: FlowTraceEvent,
        adp: AggregatedDataProfile?,
        relation: com.ritense.iko.aggregateddataprofile.domain.Relation?,
        http: FlowTraceHttpInfo?,
        httpResponse: FlowTraceMessagePayload?,
        mappedResult: String?,
        outgoing: OutgoingHttpCapture?,
    ): List<FlowTraceField> {
        val fields = ArrayList<FlowTraceField>()
        fun add(label: String, value: String?) {
            if (!value.isNullOrBlank()) fields.add(FlowTraceField(label, value))
        }
        fun addCode(label: String, value: String?) {
            if (!value.isNullOrBlank()) fields.add(FlowTraceField(label, value, code = true))
        }

        val profile = rep.variables[PROFILE_NAME]
        val profileV = rep.variables[PROFILE_VERSION]
        val adpLabel = if (profile != null && profileV != null) "$profile v$profileV" else profile
        fun addConnector() {
            val connTag = rep.variables[CONNECTOR_TAG_VARIABLE]
            val connV = rep.variables[CONNECTOR_VERSION_VARIABLE]
            add("Connector", if (connTag != null && connV != null) "$connTag v$connV" else connTag)
            add("Instance", rep.variables[CONNECTOR_INSTANCE_TAG_VARIABLE])
            add("Operation", rep.variables[CONNECTOR_OPERATION_VARIABLE])
        }

        when (type) {
            FlowTraceStepType.ADP_ENTRY, FlowTraceStepType.ADP_RESULT -> add("ADP", adpLabel)

            FlowTraceStepType.ADP_ENDPOINT_TRANSFORM -> {
                add("ADP", adpLabel)
                addCode("JQ", adp?.endpointTransform?.expression)
                addCode(
                    "Context",
                    rep.headers[ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER]
                        ?: rep.variables[ENDPOINT_TRANSFORM_CONTEXT_VARIABLE],
                )
                addCode("Result", mappedResult)
            }

            FlowTraceStepType.ADP_RESULT_TRANSFORM -> {
                add("ADP", adpLabel)
                addCode("JQ", adp?.resultTransform?.expression)
            }

            FlowTraceStepType.AGGREGATION -> {
                add("ADP", adpLabel)
                add("Merge", "relation results")
            }

            FlowTraceStepType.CONNECTOR_TRANSFORM -> addConnector()

            FlowTraceStepType.CONNECTOR_OPERATION -> addConnector()

            FlowTraceStepType.HTTP -> {
                add("Method", http?.method)
                add("Status", http?.status?.toString())
                add("Status text", http?.statusText)
                add("Outgoing URL", outgoing?.uri)
                addCode(
                    "Outgoing headers",
                    outgoing?.headers?.takeIf { it.isNotEmpty() }
                        ?.let { headerRedactor.redact(it) }
                        ?.entries?.joinToString("\n") { (k, v) -> "$k: $v" },
                )
                addCode("Outgoing body", outgoing?.body)
                addCode(
                    "Response headers",
                    httpResponse?.headers?.takeIf { it.isNotEmpty() }
                        ?.entries?.joinToString("\n") { (k, v) -> "$k: $v" },
                )
                addCode("Response body", httpResponse?.body)
            }

            FlowTraceStepType.RELATION_START -> add("Relation", relation?.propertyName)

            FlowTraceStepType.RELATION_SOURCE_MAPPING -> {
                add("Relation", relation?.propertyName)
                addCode("JQ", relation?.endpointTransform?.expression)
                addCode("Context", rep.variables[ENDPOINT_TRANSFORM_CONTEXT_VARIABLE])
                addCode("Result", mappedResult)
            }

            FlowTraceStepType.RELATION_RESULT_TRANSFORM -> {
                add("Relation", relation?.propertyName)
                addCode("JQ", relation?.resultTransform?.expression)
            }

            FlowTraceStepType.RELATION_RESULT -> add("Relation", relation?.propertyName)
        }
        return fields
    }

    private data class Step(
        val rep: FlowTraceEvent,
        val name: String,
        val stepId: String,
        val type: FlowTraceStepType,
        val label: String,
        val category: FlowTraceCategory,
        val status: String,
        val elapsed: Long,
        val entity: String?,
        val subroute: String,
        val detail: FlowTraceStep,
    )

    /** Parse the HTTP status from an `HttpOperationFailedException` message (`... with statusCode: 500`)
     *  — the fallback when a failed call left no `CamelHttpResponseCode` header / debug variable. */
    private fun statusFromException(ex: FlowTraceExceptionInfo?): Int? = ex?.message?.let { STATUS_CODE_REGEX.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }

    /** Reverse the `"k: v"`-per-line encoding the error handler uses for the captured failed-response
     *  headers (see ErrorHelper) back into a header map. */
    private fun parseHeaderLines(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.lineSequence().mapNotNull { line ->
            val i = line.indexOf(": ")
            if (i > 0) line.substring(0, i) to line.substring(i + 2) else null
        }.toMap()
    }

    companion object {
        private const val HTTP_RESPONSE_CODE_HEADER = "CamelHttpResponseCode"
        private const val HTTP_RESPONSE_TEXT_HEADER = "CamelHttpResponseText"
        private const val HTTP_METHOD_HEADER = "CamelHttpMethod"

        private const val REST_OPENAPI_MARKER = "rest-openapi"
        private val STATUS_CODE_REGEX = Regex("statusCode:\\s*(\\d+)")

        private const val CONNECTOR_ROUTE_PREFIX = "connector:"

        private const val CONNECTOR_TRANSFORM_MARKER = "transform"

        private const val ENDPOINT_TRANSFORM_RESULT_MARKER = "endpointTransformResult"

        private val ADP_ROOT_ROUTE = Regex("^adp:.+:route-root$")
        private val ADP_ENDPOINT_TRANSFORM_ROUTE = Regex("^adp:.+:endpoint-transform$")
        private val RELATION_ROOT_ROUTE = Regex("^relation:(.+):root$")
        private val RELATION_MAP_ARRAY_ROUTE = Regex("^relation:(.+):(map|array)$")

        private val RELATION_ROUTE = Regex("^relation:(.+):(root|map|array|loop)$")
    }
}