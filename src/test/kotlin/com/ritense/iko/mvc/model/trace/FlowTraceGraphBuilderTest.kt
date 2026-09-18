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

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.EndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.aggregateddataprofile.domain.RelationEndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Transform
import com.ritense.iko.camel.OutgoingHttpCapture
import org.apache.camel.spi.BacklogTracerEventMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class FlowTraceGraphBuilderTest {

    private val objectMapper = ObjectMapper()

    // Header display on, so tests asserting header content stay meaningful; masking has its own test.
    private val builder = FlowTraceGraphBuilder(
        FlowTraceEventParser(objectMapper),
        HeaderRedactor(DebugTraceProperties(showHeaders = true)),
    )

    private val rootRoute = "adp:personen:1.0.0:route-root"
    private val transformRoute = "adp:personen:1.0.0:endpoint-transform"
    private val connectorRoute = "connector:zaken:1.0.0:direct:iko:connector:zaken"

    private val ownerRelation: Relation = relation("owner", "{\"id\": .source.ownerId}", ".[0].name")
    private val documentsRelation: Relation = relation("documents", "{\"id\": .source.id}", ".")

    private val adp: AggregatedDataProfile = mock<AggregatedDataProfile>().also {
        // Stub outside a stubbing lambda so nested relation mocks don't trip Mockito's
        // UnfinishedStubbingException.
        org.mockito.kotlin.whenever(it.endpointTransform).thenReturn(EndpointTransform("{\"id\": .id}"))
        org.mockito.kotlin.whenever(it.resultTransform).thenReturn(Transform("map(.name)"))
        org.mockito.kotlin.whenever(it.relations).thenReturn(mutableListOf(ownerRelation, documentsRelation))
    }

    private fun relation(property: String, sourceMapping: String, resultTransform: String): Relation = mock<Relation>().also {
        org.mockito.kotlin.whenever(it.propertyName).thenReturn(property)
        org.mockito.kotlin.whenever(it.endpointTransform).thenReturn(RelationEndpointTransform(sourceMapping))
        org.mockito.kotlin.whenever(it.resultTransform).thenReturn(Transform(resultTransform))
    }

    @Test
    fun `builds the trunk pipeline as typed nodes with a paired http response and jq from config`() {
        val vars = mapOf("profileName" to "personen", "profileVersion" to "1.0.0")
        val connVars = vars + mapOf("connectorTag" to "zaken", "connectorVersion" to "1.0.0", "operation" to "zaak_list")
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = vars),
            event(2, "E1", transformRoute, "setVariable", "setVariable[endpointTransformResult]", variables = vars),
            event(3, "E1", connectorRoute, "from", body = "{}", variables = connVars),
            // the real HTTP call: a rest-openapi toD node inside the connector operation route
            event(4, "E1", connectorRoute, "toD", "toD[language:groovy:\"rest-openapi:...\"]", body = "{}", variables = connVars),
            event(
                5,
                "E1",
                connectorRoute,
                "unmarshal",
                "unmarshal[json]",
                body = "{\"id\":42}",
                headers = mapOf("CamelHttpResponseCode" to "200", "CamelHttpMethod" to "GET", "CamelHttpResponseText" to "OK"),
                variables = connVars,
            ),
            event(6, "E1", rootRoute, "transform", "transform[jq{map(.name)}]", body = "[{\"name\":\"a\"}]", variables = vars),
            event(7, "E1", rootRoute, "marshal", "marshal[json]", body = "[\"a\"]", variables = vars),
        )

        val graph = builder.build(messages, adp)

        assertThat(graph.steps.values.map { it.type }).containsExactlyInAnyOrder(
            FlowTraceStepType.ADP_ENTRY,
            FlowTraceStepType.ADP_ENDPOINT_TRANSFORM,
            FlowTraceStepType.CONNECTOR_OPERATION,
            FlowTraceStepType.HTTP,
            FlowTraceStepType.ADP_RESULT_TRANSFORM,
            FlowTraceStepType.ADP_RESULT,
        )
        val entry = graph.nodes.single { it.category == FlowTraceCategory.ENTRY }
        assertThat(entry.label).isEqualTo("profile root")
        // The entry (header banner) carries the final result (last step) as its exchange output.
        assertThat(graph.steps.getValue(entry.stepId).output?.body).isEqualTo("[\"a\"]")

        // HTTP facts live on the HTTP step (the rest-openapi call), not the connector operation.
        val http = graph.steps.values.single { it.type == FlowTraceStepType.HTTP }
        assertThat(http.http?.status).isEqualTo(200)
        assertThat(http.http?.method).isEqualTo("GET")
        assertThat(http.http?.statusText).isEqualTo("OK")
        assertThat(http.output?.body).isEqualTo("{\"id\":42}")
        // Details carries the response body (from the CamelHttp* response on success).
        assertThat(http.fields).anySatisfy {
            assertThat(it.label).isEqualTo("Response body")
            assertThat(it.value).isEqualTo("{\"id\":42}")
            assertThat(it.code).isTrue()
        }

        // The connector operation step no longer carries http; it identifies the operation.
        val op = graph.steps.values.single { it.type == FlowTraceStepType.CONNECTOR_OPERATION }
        assertThat(op.http).isNull()
        assertThat(op.fields).anySatisfy {
            assertThat(it.label).isEqualTo("Operation")
            assertThat(it.value).isEqualTo("zaak_list")
        }

        // JQ expressions come from the ADP config, rendered as code fields.
        val endpointTransform = graph.steps.values.single { it.type == FlowTraceStepType.ADP_ENDPOINT_TRANSFORM }
        assertThat(endpointTransform.fields).anySatisfy {
            assertThat(it.label).isEqualTo("JQ")
            assertThat(it.value).isEqualTo("{\"id\": .id}")
            assertThat(it.code).isTrue()
        }
        val resultTransform = graph.steps.values.single { it.type == FlowTraceStepType.ADP_RESULT_TRANSFORM }
        assertThat(resultTransform.fields.single { it.code }.value).isEqualTo("map(.name)")

        // A straight chain entry -> transform -> op -> http -> result-transform -> result (acyclic).
        assertThat(graph.links).hasSize(5)
        assertThat(isAcyclic(graph)).isTrue()
    }

    @Test
    fun `output pairs to the next same-exchange event body`() {
        val vars = mapOf("profileName" to "personen")
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = vars),
            event(2, "E1", rootRoute, "transform", "transform[jq{.}]", body = "[{\"name\":\"a\"}]", variables = vars),
            event(3, "E1", rootRoute, "marshal", "marshal[json]", body = "[\"a\"]", variables = vars),
        )

        val graph = builder.build(messages, adp)

        // The result-transform's output is the body entering the next node (marshal).
        val transform = graph.steps.values.single { it.type == FlowTraceStepType.ADP_RESULT_TRANSFORM }
        assertThat(transform.input?.body).isEqualTo("[{\"name\":\"a\"}]")
        assertThat(transform.output?.body).isEqualTo("[\"a\"]")
    }

    @Test
    fun `http response body comes from the first status event that carries a body, not the empty unwind`() {
        val connVars = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = mapOf("profileName" to "personen")),
            event(2, "E1", connectorRoute, "from", body = "{}", variables = connVars),
            // request snapshot entering the toD (no status yet)
            event(3, "E1", connectorRoute, "toD", "toD[rest-openapi]", body = "{}", variables = connVars),
            // first status-bearing event carries the response body
            event(
                4,
                "E1",
                connectorRoute,
                "log",
                "log[body]",
                body = "[{\"id\":1}]",
                headers = mapOf("CamelHttpResponseCode" to "200", "CamelHttpMethod" to "GET"),
                variables = connVars,
            ),
            // later unwind `from` still has the status header but an empty body (maxByOrNull would pick this)
            event(
                5,
                "E1",
                connectorRoute,
                "from",
                body = "",
                headers = mapOf("CamelHttpResponseCode" to "200"),
                variables = connVars,
            ),
        )

        val graph = builder.build(messages, adp)

        val http = graph.steps.values.single { it.type == FlowTraceStepType.HTTP }
        assertThat(http.output?.body).isEqualTo("[{\"id\":1}]")
        assertThat(http.http?.status).isEqualTo(200)
        assertThat(http.http?.method).isEqualTo("GET")
    }

    @Test
    fun `failed http step carries the exception, status from the message and the debug-captured response`() {
        val connVars = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")
        // The error handler stashes the failed response as debug variables (see ErrorHelper); they
        // surface on the connector route's unwind `from` event.
        val debugVars = connVars + mapOf(
            "iko_debug_httpResponseCode" to "500",
            "iko_debug_httpResponseText" to "Server Error",
            "iko_debug_httpResponseBody" to "{\"detail\":\"boom\"}",
            "iko_debug_httpResponseHeaders" to "Content-Type: application/json",
        )
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}"),
            event(2, "E1", connectorRoute, "from", body = "{}", variables = connVars),
            event(
                3,
                "E1",
                connectorRoute,
                "toD",
                "toD[language:groovy:\"rest-openapi:...\"]",
                body = "{}",
                headers = mapOf("CamelHttpMethod" to "GET"),
                variables = connVars,
                failed = true,
                done = false,
                exceptionJson = """{"exception":{"type":"org.apache.camel.http.base.HttpOperationFailedException","message":"HTTP operation failed invoking https://x with statusCode: 500","stackTrace":"line1\nline2"}}""",
            ),
            // unwind carrying the response status header (body is still the request `{}`) AND the
            // debug-captured response variables — the debug body must win over this event's `{}`.
            event(
                4,
                "E1",
                connectorRoute,
                "from",
                body = "{}",
                headers = mapOf("CamelHttpResponseCode" to "500", "CamelHttpResponseText" to "Server Error"),
                variables = debugVars,
                failed = true,
                done = false,
            ),
        )

        val graph = builder.build(messages, adp)

        val http = graph.steps.values.single { it.type == FlowTraceStepType.HTTP }
        assertThat(http.status).isEqualTo(FlowTraceStatus.FAILED)
        assertThat(http.http?.status).isEqualTo(500)
        assertThat(http.http?.statusText).isEqualTo("Server Error")
        assertThat(http.http?.method).isEqualTo("GET")
        // The Exchange output stays the raw exchange (still the request body after the failure).
        assertThat(http.output?.body).isEqualTo("{}")
        // The real (debug-captured) failed response body + headers are surfaced in the Details fields.
        assertThat(http.fields).anySatisfy {
            assertThat(it.label).isEqualTo("Response body")
            assertThat(it.value).isEqualTo("{\"detail\":\"boom\"}")
            assertThat(it.code).isTrue()
        }
        assertThat(http.fields).anySatisfy {
            assertThat(it.label).isEqualTo("Response headers")
            assertThat(it.value).contains("Content-Type: application/json")
        }
        assertThat(http.exception?.type).isEqualTo("org.apache.camel.http.base.HttpOperationFailedException")
        // Under per-event attribution only links touching the failed HTTP step are red; the upstream
        // entry -> operation link (a step that did not itself fail) is neutral.
        val httpNode = graph.nodes.single { it.type == FlowTraceStepType.HTTP }
        assertThat(graph.links.filter { it.target == httpNode.name })
            .isNotEmpty
            .allSatisfy { assertThat(it.status).isEqualTo(FlowTraceStatus.FAILED) }
        assertThat(graph.links.filter { it.target != httpNode.name && it.source != httpNode.name })
            .allSatisfy { assertThat(it.status).isEqualTo(FlowTraceStatus.OK) }
    }

    @Test
    fun `relation branch splits off the parent operation and merges into aggregation`() {
        val vars = mapOf("profileName" to "personen")
        val connVars = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = vars),
            event(2, "E1", connectorRoute, "from", body = "{}", variables = connVars),
            event(3, "E1", rootRoute, "enrich", "enrich[constant{direct:multicast:x}]", variables = vars),
            event(4, "E1", rootRoute, "transform", "transform[jq{.}]", variables = vars),
            event(5, "E1", rootRoute, "marshal", "marshal[json]", variables = vars),
            // multicast shell exchange (no business node) sits between trunk and relation.
            event(10, "SHELL", "adp:personen:1.0.0:multicast", "from", correlation = "E1"),
            event(11, "SHELL", "adp:personen:1.0.0:multicast", "multicast", "multicast", correlation = "E1"),
            // relation on its own exchange, correlated to the shell.
            event(12, "E2", "relation:owner:root", "from", correlation = "SHELL", body = "{\"ownerId\":1}"),
            event(
                13,
                "E2",
                "relation:owner:root",
                "setVariable",
                "setVariable[endpointTransformResult]",
                correlation = "SHELL",
                variables = mapOf("endpointTransformContext" to "{\"source\":{\"ownerId\":1}}"),
            ),
            event(14, "E2", connectorRoute, "from", correlation = "SHELL", variables = connVars),
            event(15, "E2", "relation:owner:map", "transform", "transform[jq{.[0].name}]", correlation = "SHELL"),
            event(16, "E2", "relation:owner:map", "unmarshal", "unmarshal[json]", correlation = "SHELL", body = "\"Ann\""),
        )

        val graph = builder.build(messages, adp)

        val relationStart = graph.steps.values.single { it.type == FlowTraceStepType.RELATION_START }
        assertThat(relationStart.label).isEqualTo("relation processed")
        // The relation header carries the relation's result (post result-transform) as its exchange output.
        assertThat(relationStart.output?.body).isEqualTo("\"Ann\"")
        assertThat(graph.steps.values.map { it.type }).contains(
            FlowTraceStepType.RELATION_SOURCE_MAPPING,
            FlowTraceStepType.RELATION_RESULT_TRANSFORM,
            FlowTraceStepType.RELATION_RESULT,
            FlowTraceStepType.AGGREGATION,
        )
        // Source mapping carries the relation jq + the endpointTransformContext (both as code).
        val mapping = graph.steps.values.single { it.type == FlowTraceStepType.RELATION_SOURCE_MAPPING }
        assertThat(mapping.fields).anySatisfy {
            assertThat(it.label).isEqualTo("JQ")
            assertThat(it.value).isEqualTo("{\"id\": .source.ownerId}")
            assertThat(it.code).isTrue()
        }
        assertThat(mapping.fields).anySatisfy {
            assertThat(it.label).isEqualTo("Context")
            assertThat(it.code).isTrue()
        }
        // The aggregation node is ordered after the relation branch it merges (not at the enrich's
        // start uid, which precedes the relations).
        val order = graph.nodes.map { it.stepId }
        val aggregationNode = graph.nodes.single { it.category == FlowTraceCategory.AGGREGATION }
        val relationResultStep = graph.steps.values.single { it.type == FlowTraceStepType.RELATION_RESULT }
        assertThat(order.indexOf(aggregationNode.stepId))
            .isGreaterThan(order.indexOf(relationResultStep.stepId))

        val nodeByStep = graph.nodes.associateBy { it.stepId }
        val startNode = nodeByStep.getValue(relationStart.stepId)
        // The relation start is fed by the parent's HTTP operation (split), folding out the shell.
        val parentOp = graph.steps.values.first { it.type == FlowTraceStepType.CONNECTOR_OPERATION && it.branch == "E1" }
        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(nodeByStep.getValue(parentOp.stepId).name)
            assertThat(it.target).isEqualTo(startNode.name)
        }
        // The relation result merges into the aggregation node.
        val aggregation = graph.nodes.single { it.category == FlowTraceCategory.AGGREGATION }
        val relationResultNode = nodeByStep.getValue(graph.steps.values.single { it.type == FlowTraceStepType.RELATION_RESULT }.stepId)
        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(relationResultNode.name)
            assertThat(it.target).isEqualTo(aggregation.name)
        }
        assertThat(isAcyclic(graph)).isTrue()
    }

    @Test
    fun `nested relation failure is attributed to the failing branch and its aggregations only`() {
        val vars = mapOf("profileName" to "personen", "profileVersion" to "1.0.0")
        val connVars = vars + mapOf("connectorTag" to "zaken", "connectorVersion" to "1.0.0", "operation" to "zaak_read")
        val messages = listOf(
            // trunk (own fetch succeeds; the aggregation fails once tier-1 unwinds the exception).
            event(1, "E1", rootRoute, "from", body = "{}", variables = vars),
            event(2, "E1", transformRoute, "setVariable", "setVariable[endpointTransformResult]", variables = vars),
            event(3, "E1", connectorRoute, "from", variables = connVars),
            event(4, "E1", connectorRoute, "toD", "toD[rest-openapi]", headers = mapOf("CamelHttpResponseCode" to "200"), variables = connVars),
            event(6, "E1", rootRoute, "enrich", "enrich[constant{direct:multicast:trunk}]", variables = vars, failed = true, done = true),
            event(7, "E1", rootRoute, "transform", "transform[jq{.}]", variables = vars),
            event(8, "E1", rootRoute, "marshal", "marshal[json]", body = "{}", variables = vars),
            // shell between trunk and tier-1.
            event(10, "S1", "adp:personen:1.0.0:multicast", "from", correlation = "E1"),
            event(11, "S1", "adp:personen:1.0.0:multicast", "multicast", "multicast", correlation = "E1"),
            // tier-1 relation `owner` (map/single): own fetch succeeds, its aggregation fails.
            event(12, "E2", "relation:owner:root", "from", correlation = "S1", body = "{\"ownerId\":1}"),
            event(13, "E2", "relation:owner:root", "setVariable", "setVariable[endpointTransformResult]", correlation = "S1"),
            event(14, "E2", connectorRoute, "from", correlation = "S1", variables = connVars),
            event(15, "E2", connectorRoute, "toD", "toD[rest-openapi]", correlation = "S1", headers = mapOf("CamelHttpResponseCode" to "200"), variables = connVars),
            event(16, "E2", "relation:owner:loop", "enrich", "enrich[constant{direct:multicast:owner}]", correlation = "S1", failed = true, done = true),
            event(17, "E2", "relation:owner:map", "transform", "transform[jq{.}]", correlation = "S1"),
            event(18, "E2", "relation:owner:map", "unmarshal", "unmarshal[json]", correlation = "S1", body = "\"Ann\""),
            // shell between tier-1 and tier-2.
            event(20, "S2", "relation:owner:multicast", "from", correlation = "E2"),
            event(21, "S2", "relation:owner:multicast", "multicast", "multicast", correlation = "E2"),
            // tier-2 relation `documents`: the HTTP call throws.
            event(22, "E3", "relation:documents:root", "from", correlation = "S2", body = "{\"id\":1}"),
            event(23, "E3", "relation:documents:root", "setVariable", "setVariable[endpointTransformResult]", correlation = "S2"),
            event(24, "E3", connectorRoute, "from", correlation = "S2", variables = connVars),
            event(
                25,
                "E3",
                connectorRoute,
                "toD",
                "toD[rest-openapi]",
                correlation = "S2",
                headers = mapOf("CamelHttpMethod" to "GET"),
                variables = connVars,
                failed = true,
                done = true,
                exceptionJson = """{"exception":{"type":"org.apache.camel.http.base.HttpOperationFailedException","message":"HTTP operation failed invoking https://x with statusCode: 500","stackTrace":"line1"}}""",
            ),
        )

        val graph = builder.build(messages, adp)

        fun step(type: FlowTraceStepType, branch: String) = graph.steps.values.single { it.type == type && it.branch == branch }

        // Failure is attributed only to the tier-2 HTTP and to each aggregation that folds it in.
        assertThat(step(FlowTraceStepType.HTTP, "E3").status).isEqualTo(FlowTraceStatus.FAILED)
        assertThat(step(FlowTraceStepType.HTTP, "E3").exception?.type)
            .isEqualTo("org.apache.camel.http.base.HttpOperationFailedException")
        // tier-1's own steps did not fail.
        assertThat(step(FlowTraceStepType.RELATION_SOURCE_MAPPING, "E2").status).isEqualTo(FlowTraceStatus.OK)
        assertThat(step(FlowTraceStepType.CONNECTOR_OPERATION, "E2").status).isEqualTo(FlowTraceStatus.OK)
        assertThat(step(FlowTraceStepType.HTTP, "E2").status).isEqualTo(FlowTraceStatus.OK)
        // both aggregations (tier-1's own and the trunk's) surface the propagated failure.
        assertThat(step(FlowTraceStepType.AGGREGATION, "E2").status).isEqualTo(FlowTraceStatus.FAILED)
        assertThat(step(FlowTraceStepType.AGGREGATION, "E1").status).isEqualTo(FlowTraceStatus.FAILED)

        // Per-level aggregation: the tier-1 aggregation carries the relation property, the trunk's the ADP.
        val relAgg = graph.nodes.single { it.category == FlowTraceCategory.AGGREGATION && it.branch == "E2" }
        val trunkAgg = graph.nodes.single { it.category == FlowTraceCategory.AGGREGATION && it.branch == "E1" }
        assertThat(relAgg.entity).isEqualTo("owner")
        assertThat(trunkAgg.entity).isEqualTo("personen")

        // Topology: tier-2 merges into the tier-1 aggregation; tier-1 merges into the trunk aggregation.
        val nodeByStep = graph.nodes.associateBy { it.stepId }
        val tier2Http = nodeByStep.getValue(step(FlowTraceStepType.HTTP, "E3").stepId)
        val tier1Result = nodeByStep.getValue(step(FlowTraceStepType.RELATION_RESULT, "E2").stepId)
        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(tier2Http.name)
            assertThat(it.target).isEqualTo(relAgg.name)
        }
        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(tier1Result.name)
            assertThat(it.target).isEqualTo(trunkAgg.name)
        }
        // tier-2 splits off tier-1's HTTP.
        val tier1Http = nodeByStep.getValue(step(FlowTraceStepType.HTTP, "E2").stepId)
        val tier2Start = nodeByStep.getValue(step(FlowTraceStepType.RELATION_START, "E3").stepId)
        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(tier1Http.name)
            assertThat(it.target).isEqualTo(tier2Start.name)
        }
        assertThat(isAcyclic(graph)).isTrue()
    }

    @Test
    fun `array relation iteration steps fold inline into the relation branch`() {
        val vars = mapOf("profileName" to "personen")
        val connVars = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")
        val docTransformRoute = "connector:zaken:1.0.0:direct:iko:endpoint:transform:zaken.doc_read"
        val rel = mapOf("relationPropertyName" to "documents", "connectorTag" to "zaken", "operation" to "doc_read")
        val messages = listOf(
            // trunk: own fetch feeds the array relation, then the aggregation.
            event(1, "E1", rootRoute, "from", body = "{}", variables = vars),
            event(2, "E1", connectorRoute, "from", variables = connVars),
            event(3, "E1", connectorRoute, "toD", "toD[rest-openapi]", headers = mapOf("CamelHttpResponseCode" to "200"), variables = connVars),
            event(4, "E1", rootRoute, "enrich", "enrich[constant{direct:multicast:trunk}]", variables = vars),
            event(5, "E1", rootRoute, "transform", "transform[jq{.}]", variables = vars),
            event(6, "E1", rootRoute, "marshal", "marshal[json]", body = "[]", variables = vars),
            // multicast shell.
            event(10, "S1", "adp:personen:1.0.0:multicast", "from", correlation = "E1"),
            event(11, "S1", "adp:personen:1.0.0:multicast", "multicast", "multicast", correlation = "E1"),
            // array relation root/`:array` driver exchange.
            event(12, "R", "relation:documents:root", "from", correlation = "S1", body = "{\"id\":1}"),
            event(13, "R", "relation:documents:root", "setVariable", "setVariable[endpointTransformResult]", correlation = "S1"),
            // iteration child 1.
            event(14, "L1", docTransformRoute, "from", correlation = "R", variables = rel),
            event(15, "L1", connectorRoute, "from", correlation = "R", variables = rel),
            event(16, "L1", connectorRoute, "toD", "toD[rest-openapi]", correlation = "R", headers = mapOf("CamelHttpResponseCode" to "200"), variables = rel),
            // iteration child 2 (collapses into child 1).
            event(17, "L2", docTransformRoute, "from", correlation = "R", variables = rel),
            event(18, "L2", connectorRoute, "from", correlation = "R", variables = rel),
            event(19, "L2", connectorRoute, "toD", "toD[rest-openapi]", correlation = "R", headers = mapOf("CamelHttpResponseCode" to "200"), variables = rel),
            // array result: `:array` transform + unmarshal, back on the root exchange.
            event(30, "R", "relation:documents:array", "transform", "transform[jq{.}]", correlation = "S1", body = "[]"),
            event(31, "R", "relation:documents:array", "unmarshal", "unmarshal[json]", correlation = "S1", body = "[]"),
        )

        val graph = builder.build(messages, adp)

        // The iteration steps run on child exchanges (L1, L2) but are folded onto the relation-root lane
        // (R): no node keeps an iteration exchange as its branch, and the two iterations collapse to one
        // representative carrying the ×N count.
        assertThat(graph.nodes.map { it.branch }).doesNotContain("L1", "L2")
        val iterFirst = graph.nodes.single { it.type == FlowTraceStepType.CONNECTOR_TRANSFORM }
        assertThat(iterFirst.branch).isEqualTo("R")
        assertThat(iterFirst.iterations).isEqualTo(2)

        // Look up by node branch (the folded lane), not the step's own exchange.
        fun node(type: FlowTraceStepType, branch: String) = graph.nodes.single { it.type == type && it.branch == branch }

        // Inline chain within the relation branch: source mapping -> iteration steps -> result transform.
        val mapping = node(FlowTraceStepType.RELATION_SOURCE_MAPPING, "R")
        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(mapping.name)
            assertThat(it.target).isEqualTo(iterFirst.name)
        }
        val iterHttp = node(FlowTraceStepType.HTTP, "R")
        val resultTransform = node(FlowTraceStepType.RELATION_RESULT_TRANSFORM, "R")
        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(iterHttp.name)
            assertThat(it.target).isEqualTo(resultTransform.name)
        }
        // The relation as a whole splits off the trunk's HTTP and merges into the trunk aggregation.
        val relationStart = node(FlowTraceStepType.RELATION_START, "R")
        val trunkHttp = node(FlowTraceStepType.HTTP, "E1")
        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(trunkHttp.name)
            assertThat(it.target).isEqualTo(relationStart.name)
        }
        val relationResult = node(FlowTraceStepType.RELATION_RESULT, "R")
        val trunkAgg = graph.nodes.single { it.category == FlowTraceCategory.AGGREGATION && it.branch == "E1" }
        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(relationResult.name)
            assertThat(it.target).isEqualTo(trunkAgg.name)
        }
        assertThat(isAcyclic(graph)).isTrue()
    }

    @Test
    fun `aggregation and result transform prefer the captured debug json bodies`() {
        val vars = mapOf("profileName" to "personen")
        val connVars = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = vars),
            event(2, "E1", connectorRoute, "from", variables = connVars),
            // enrich (aggregation): its own event carries the pre-aggregation JSON snapshot.
            event(
                3,
                "E1",
                rootRoute,
                "enrich",
                "enrich[constant{direct:multicast:x}]",
                body = "{left=…}",
                variables = vars + mapOf("iko_debug_preAggregationJson" to "{\"agg\":\"in\"}"),
            ),
            // result transform: its event carries the aggregation output / result-transform input JSON.
            event(
                4,
                "E1",
                rootRoute,
                "transform",
                "transform[jq{.}]",
                body = "{left=…}",
                variables = vars + mapOf("iko_debug_preResultTransformJson" to "{\"agg\":\"out\"}"),
            ),
            event(5, "E1", rootRoute, "marshal", "marshal[json]", body = "[\"a\"]", variables = vars),
        )

        val graph = builder.build(messages, adp)

        val aggregation = graph.steps.values.single { it.type == FlowTraceStepType.AGGREGATION }
        assertThat(aggregation.input?.body).isEqualTo("{\"agg\":\"in\"}")
        assertThat(aggregation.output?.body).isEqualTo("{\"agg\":\"out\"}")
        val resultTransform = graph.steps.values.single { it.type == FlowTraceStepType.ADP_RESULT_TRANSFORM }
        assertThat(resultTransform.input?.body).isEqualTo("{\"agg\":\"out\"}")
        // Without the debug vars, bodies fall back to the raw traced body (marshal result stays raw JSON).
        val result = graph.steps.values.single { it.type == FlowTraceStepType.ADP_RESULT }
        assertThat(result.input?.body).isEqualTo("[\"a\"]")
    }

    @Test
    fun `relation splits off the parent HTTP response, not the connector operation`() {
        val vars = mapOf("profileName" to "personen")
        val connVars = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = vars),
            event(2, "E1", connectorRoute, "from", body = "{}", variables = connVars),
            event(
                3,
                "E1",
                connectorRoute,
                "toD",
                "toD[language:groovy:\"rest-openapi:...\"]",
                body = "{}",
                headers = mapOf("CamelHttpResponseCode" to "200", "CamelHttpMethod" to "GET"),
                variables = connVars,
            ),
            event(4, "E1", rootRoute, "enrich", "enrich[constant{direct:multicast:x}]", variables = vars),
            event(5, "E1", rootRoute, "transform", "transform[jq{.}]", variables = vars),
            event(6, "E1", rootRoute, "marshal", "marshal[json]", variables = vars),
            event(12, "E2", "relation:owner:root", "from", correlation = "E1", body = "{\"ownerId\":1}"),
            event(13, "E2", "relation:owner:root", "setVariable", "setVariable[endpointTransformResult]", correlation = "E1"),
        )

        val graph = builder.build(messages, adp)

        val nodeByStep = graph.nodes.associateBy { it.stepId }
        val http = graph.steps.values.single { it.type == FlowTraceStepType.HTTP && it.branch == "E1" }
        val op = graph.steps.values.single { it.type == FlowTraceStepType.CONNECTOR_OPERATION && it.branch == "E1" }
        val relationStart = graph.steps.values.single { it.type == FlowTraceStepType.RELATION_START }

        assertThat(graph.links).anySatisfy {
            assertThat(it.source).isEqualTo(nodeByStep.getValue(http.stepId).name)
            assertThat(it.target).isEqualTo(nodeByStep.getValue(relationStart.stepId).name)
        }
        assertThat(graph.links).noneSatisfy {
            assertThat(it.source).isEqualTo(nodeByStep.getValue(op.stepId).name)
            assertThat(it.target).isEqualTo(nodeByStep.getValue(relationStart.stepId).name)
        }
    }

    @Test
    fun `source mapping surfaces the produced endpointTransformResult as a Result field`() {
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = mapOf("profileName" to "personen")),
            event(2, "E1", connectorRoute, "from", body = "{}", variables = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")),
            event(10, "E2", "relation:owner:root", "from", correlation = "E1", body = "{\"ownerId\":1}"),
            event(11, "E2", "relation:owner:root", "setVariable", "setVariable[endpointTransformResult]", correlation = "E1"),
            // the result variable is only visible on the event AFTER the setVariable node
            event(
                12,
                "E2",
                "relation:owner:root",
                "process",
                "Processor",
                correlation = "E1",
                variables = mapOf("endpointTransformResult" to "{\"zaak\":\"https://example/zaken/1\"}"),
            ),
        )

        val graph = builder.build(messages, adp)

        val mapping = graph.steps.values.single { it.type == FlowTraceStepType.RELATION_SOURCE_MAPPING }
        assertThat(mapping.fields).anySatisfy {
            assertThat(it.label).isEqualTo("Result")
            assertThat(it.value).isEqualTo("{\"zaak\":\"https://example/zaken/1\"}")
            assertThat(it.code).isTrue()
        }
    }

    @Test
    fun `http step surfaces the captured outgoing request keyed by exchange id`() {
        val connVars = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = mapOf("profileName" to "personen")),
            event(2, "E1", connectorRoute, "from", body = "{}", variables = connVars),
            event(
                3,
                "E1",
                connectorRoute,
                "toD",
                "toD[rest-openapi]",
                body = "{}",
                headers = mapOf("CamelHttpResponseCode" to "200", "CamelHttpMethod" to "GET"),
                variables = connVars,
            ),
        )
        val outgoing = listOf(
            OutgoingHttpCapture(
                spanId = "E1",
                method = "GET",
                uri = "http://host/zaken?zaak=1",
                headers = mapOf("Accept" to "application/json"),
                body = null,
            ),
        )

        val graph = builder.build(messages, adp, outgoing)

        val http = graph.steps.values.single { it.type == FlowTraceStepType.HTTP }
        assertThat(http.fields).anySatisfy {
            assertThat(it.label).isEqualTo("Outgoing URL")
            assertThat(it.value).isEqualTo("http://host/zaken?zaak=1")
        }
        assertThat(http.fields).anySatisfy {
            assertThat(it.label).isEqualTo("Outgoing headers")
            assertThat(it.value).contains("Accept: application/json")
            assertThat(it.code).isTrue()
        }
    }

    @Test
    fun `entry and relation start are never marked failed`() {
        val connVars = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = mapOf("profileName" to "personen")),
            event(
                2,
                "E1",
                connectorRoute,
                "from",
                body = "{}",
                variables = connVars,
                failed = true,
                done = false,
            ),
            event(3, "E2", "relation:owner:root", "from", correlation = "E1", failed = true, done = false),
        )

        val graph = builder.build(messages, adp)

        assertThat(graph.steps.values.single { it.type == FlowTraceStepType.ADP_ENTRY }.status).isEqualTo(FlowTraceStatus.OK)
        assertThat(graph.steps.values.single { it.type == FlowTraceStepType.RELATION_START }.status).isEqualTo(FlowTraceStatus.OK)
    }

    @Test
    fun `nodes carry their exchange as branch, trunk first and relation distinct`() {
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = mapOf("profileName" to "personen")),
            event(2, "E1", connectorRoute, "from"),
            event(10, "E2", "relation:owner:root", "from", correlation = "E1"),
        )

        val graph = builder.build(messages, adp)

        assertThat(graph.nodes.filter { it.branch == "E1" }).isNotEmpty
        assertThat(graph.nodes.first().branch).isEqualTo("E1")
        assertThat(graph.nodes.map { it.branch }.distinct()).containsExactly("E1", "E2")

        // Each node carries its exact step type (the Flow panel's simple/advanced filter keys off it).
        val typeByStep = graph.steps.mapValues { it.value.type }
        assertThat(graph.nodes).allSatisfy {
            assertThat(it.type).isEqualTo(typeByStep[it.stepId])
        }
    }

    @Test
    fun `batch relation iterations collapse into one representative with an iteration count`() {
        val vars = mapOf("profileName" to "personen")
        val rel = mapOf("relationPropertyName" to "documents", "connectorTag" to "zaken", "operation" to "io_list")
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = "{}", variables = vars),
            event(2, "E1", connectorRoute, "from", variables = mapOf("connectorTag" to "zaken", "operation" to "zaak_read")),
            event(10, "I1", "relation:documents:root", "from", correlation = "E1", variables = rel),
            event(11, "I1", connectorRoute, "from", correlation = "E1", variables = rel),
            event(20, "I2", "relation:documents:root", "from", correlation = "E1", variables = rel),
            event(21, "I2", connectorRoute, "from", correlation = "E1", variables = rel),
            event(30, "I3", "relation:documents:root", "from", correlation = "E1", variables = rel),
            event(31, "I3", connectorRoute, "from", correlation = "E1", variables = rel),
        )

        val graph = builder.build(messages, adp)

        val iterationBranches = graph.nodes.map { it.branch }.filter { it in setOf("I1", "I2", "I3") }.distinct()
        assertThat(iterationBranches).hasSize(1)
        val representative = graph.nodes.first { it.branch in setOf("I1", "I2", "I3") }
        assertThat(representative.iterations).isEqualTo(3)
        assertThat(graph.nodes.map { it.branch }).doesNotContain("I2", "I3")
    }

    @Test
    fun `cyclic exchange correlation never yields a circular link graph`() {
        val messages = listOf(
            event(1, "E1", "relation:a:root", "from", correlation = "E2"),
            event(2, "E2", "relation:b:root", "from", correlation = "E3"),
            event(3, "E3", "relation:c:root", "from", correlation = "E1"),
        )

        val graph = builder.build(messages, adp)

        assertThat(graph.links).noneSatisfy { assertThat(it.source).isEqualTo(it.target) }
        assertThat(isAcyclic(graph)).isTrue()
    }

    @Test
    fun `oversized body is flagged as truncated`() {
        val big = "x".repeat(40_000)
        val messages = listOf(
            event(1, "E1", rootRoute, "from", body = big, bodySize = 200_000, variables = mapOf("profileName" to "p")),
        )

        val graph = builder.build(messages, adp)

        assertThat(graph.steps.values.single().input?.truncated).isTrue()
    }

    @Test
    fun `header values are masked in the trace by default`() {
        val messages = listOf(
            event(
                1,
                "E1",
                rootRoute,
                "from",
                body = "{}",
                headers = mapOf("Authorization" to "Bearer secret", "Accept" to "application/json"),
                variables = mapOf("profileName" to "personen"),
            ),
        )

        val maskingBuilder = FlowTraceGraphBuilder(
            FlowTraceEventParser(objectMapper),
            HeaderRedactor(DebugTraceProperties(showHeaders = false)),
        )
        val graph = maskingBuilder.build(messages, adp)

        val entry = graph.steps.values.single { it.type == FlowTraceStepType.ADP_ENTRY }
        assertThat(entry.input?.headers).containsExactlyInAnyOrderEntriesOf(
            mapOf("Authorization" to "***", "Accept" to "***"),
        )
    }

    @Test
    fun `empty message list yields empty graph`() {
        val graph = builder.build(emptyList(), adp)
        assertThat(graph.nodes).isEmpty()
        assertThat(graph.links).isEmpty()
        assertThat(graph.steps).isEmpty()
    }

    private fun isAcyclic(graph: FlowTraceGraph): Boolean {
        val adj = graph.links.groupBy({ it.source }, { it.target })
        val visiting = HashSet<String>()
        val done = HashSet<String>()
        fun dfs(node: String): Boolean {
            if (node in done) return true
            if (!visiting.add(node)) return false
            adj[node]?.forEach { if (!dfs(it)) return false }
            visiting.remove(node)
            done.add(node)
            return true
        }
        return graph.nodes.all { dfs(it.name) }
    }

    private fun messageJson(
        variables: Map<String, String>,
        headers: Map<String, String>,
        body: String?,
        bodySize: Long?,
    ): String {
        val message = linkedMapOf<String, Any?>()
        if (variables.isNotEmpty()) {
            message["exchangeVariables"] = variables.map { (k, v) -> mapOf("key" to k, "value" to v) }
        }
        if (headers.isNotEmpty()) {
            message["headers"] = headers.map { (k, v) -> mapOf("key" to k, "value" to v) }
        }
        if (body != null) {
            message["body"] = buildMap {
                put("type", "String")
                if (bodySize != null) put("size", bodySize)
                put("value", body)
            }
        }
        return objectMapper.writeValueAsString(mapOf("message" to message))
    }

    private fun event(
        uid: Long,
        exchangeId: String,
        routeId: String,
        shortName: String,
        nodeLabel: String? = null,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        variables: Map<String, String> = emptyMap(),
        correlation: String? = null,
        failed: Boolean = false,
        done: Boolean = true,
        elapsed: Long = 5,
        bodySize: Long? = null,
        exceptionJson: String? = null,
    ): BacklogTracerEventMessage = mock {
        on { getUid() } doReturn uid
        on { getExchangeId() } doReturn exchangeId
        on { getRouteId() } doReturn routeId
        on { getToNodeShortName() } doReturn shortName
        on { getToNodeLabel() } doReturn (nodeLabel ?: shortName)
        on { getMessageAsJSon() } doReturn messageJson(variables, headers, body, bodySize)
        on { getCorrelationExchangeId() } doReturn correlation
        on { isFailed() } doReturn failed
        on { isDone() } doReturn done
        on { getElapsed() } doReturn elapsed
        on { hasException() } doReturn (exceptionJson != null)
        on { getExceptionAsJSon() } doReturn exceptionJson
    }
}