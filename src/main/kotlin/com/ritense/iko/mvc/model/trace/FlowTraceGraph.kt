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

/**
 * Curated step graph reconstructed from Camel's raw [org.apache.camel.spi.BacklogTracerEventMessage]s.
 *
 * [nodes] and [links] drive the vertical git-branch (commit-graph) SVG renderer; [steps] holds the
 * per-step detail shown in the modal, keyed by [FlowTraceNode.stepId] / [FlowTraceLink.stepId]. The whole graph
 * is embedded as JSON in the preview fragment.
 */
data class FlowTraceGraph(
    val nodes: List<FlowTraceNode>,
    val links: List<FlowTraceLink>,
    val steps: Map<String, FlowTraceStep>,
)

/**
 * A rendered step. [category] drives the dot sizing/colour channel and [subroute] the coloured type pill;
 * [type] is the exact [FlowTraceStepType] id (the Flow panel's simple/advanced filter keys off it). [branch] is the
 * exchangeId — nodes sharing it share a lane, the trunk (ADP root exchange) being the first branch seen
 * (lane 0). [entity] is the neutral pill (ADP name, connector tag or relation property); [iterations]
 * holds N for a collapsed batch relation (shown as a `×N` pill). An array/loop relation's per-element
 * iteration steps run on a child exchange but are folded onto the relation-root's [branch] so they render
 * inline within that relation.
 */
data class FlowTraceNode(
    val name: String,
    val label: String,
    val category: FlowTraceCategory,
    val type: FlowTraceStepType,
    val status: String,
    val stepId: String,
    val branch: String,
    val entity: String? = null,
    val subroute: String? = null,
    val iterations: Int? = null,
)

data class FlowTraceLink(
    val source: String,
    val target: String,
    val status: String,
    val stepId: String,
)

/**
 * Per-step detail. The builder fills a per-type [fields] list (the modal's label/value grid) and pairs the
 * message [input] (body/headers entering the step) with its [output] (the body/headers entering the next
 * step on the same exchange). [http] is present only on [FlowTraceStepType.HTTP] and carries the method + status
 * (the tracer does not expose a resolved URL).
 */
data class FlowTraceStep(
    val stepId: String,
    val type: FlowTraceStepType,
    val label: String,
    val status: String,
    val elapsedMs: Long,
    val branch: String,
    val fields: List<FlowTraceField>,
    val input: FlowTraceMessagePayload?,
    val output: FlowTraceMessagePayload?,
    val http: FlowTraceHttpInfo?,
    val exception: FlowTraceExceptionInfo?,
)

/**
 * A single label/value row in the modal's detail grid. [code] marks a value (e.g. a JQ expression) to be
 * rendered in a code viewer rather than inline.
 */
data class FlowTraceField(val label: String, val value: String, val code: Boolean = false)

data class FlowTraceMessagePayload(
    val body: String?,
    val headers: Map<String, String>,
    val truncated: Boolean = false,
)

/**
 * HTTP facts recoverable from the tracer for an outgoing connector call. The resolved service URL /
 * protocol are not exposed by the BacklogTracer, so they are intentionally absent — the connector
 * tag/instance/operation (in [FlowTraceStep.fields]) identify the target instead.
 */
data class FlowTraceHttpInfo(val method: String?, val status: Int?, val statusText: String? = null)

data class FlowTraceExceptionInfo(val type: String?, val message: String?, val stacktrace: String?)

/**
 * The business step-types curated from the raw trace (no framework plumbing). Each owns its [category]
 * (dot colour/size channel), [subroute] (the coloured type pill) and [label] (the row's action text).
 * Enum names are serialized verbatim into the embedded graph JSON and keyed off by `trace-diagram-init.js`.
 */
enum class FlowTraceStepType(
    val category: FlowTraceCategory,
    val subroute: String,
    val label: String,
) {
    ADP_ENTRY(FlowTraceCategory.ENTRY, "entrypoint", "profile root"),
    ADP_ENDPOINT_TRANSFORM(FlowTraceCategory.TRANSFORM, "endpoint transform", "endpoint parameters mapped"),
    CONNECTOR_TRANSFORM(FlowTraceCategory.CONNECTOR, "transform route", "request transformed"),
    CONNECTOR_OPERATION(FlowTraceCategory.CONNECTOR, "operation route", "endpoint called"),
    HTTP(FlowTraceCategory.CONNECTOR, "response", "response received"),
    AGGREGATION(FlowTraceCategory.AGGREGATION, "aggregation", "relation data combined"),
    ADP_RESULT_TRANSFORM(FlowTraceCategory.TRANSFORM, "result transform", "result transformed"),
    ADP_RESULT(FlowTraceCategory.OUTPUT, "result", "profile result"),
    RELATION_START(FlowTraceCategory.RELATION, "relation", "relation processed"),
    RELATION_SOURCE_MAPPING(FlowTraceCategory.RELATION, "mapping", "parent data mapped"),
    RELATION_RESULT_TRANSFORM(FlowTraceCategory.RELATION, "result transform", "result transformed"),
    RELATION_RESULT(FlowTraceCategory.RELATION, "relation result", "relation result"),
}

enum class FlowTraceCategory {
    ENTRY,
    CONNECTOR,
    RELATION,
    TRANSFORM,
    AGGREGATION,
    OUTPUT,
}

object FlowTraceStatus {
    const val OK = "OK"
    const val FAILED = "FAILED"
    const val IN_PROGRESS = "IN_PROGRESS"
}