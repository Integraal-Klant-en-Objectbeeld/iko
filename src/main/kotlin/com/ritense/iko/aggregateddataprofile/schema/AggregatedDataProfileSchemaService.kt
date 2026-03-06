/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
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

package com.ritense.iko.aggregateddataprofile.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import net.thisptr.jackson.jq.BuiltinFunctionLoader
import net.thisptr.jackson.jq.JsonQuery
import net.thisptr.jackson.jq.Scope
import net.thisptr.jackson.jq.Versions
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

open class AggregatedDataProfileSchemaService(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val openApiMockGenerator: OpenApiMockGenerator,
    private val jsonSchemaInferrer: JsonSchemaInferrer,
    private val mapper: ObjectMapper,
) {
    @Transactional
    open fun generateAndSave(adpId: UUID) {
        val adp = aggregatedDataProfileRepository.findById(adpId)
            .orElseThrow { NoSuchElementException("ADP not found: $adpId") }
        if (!isSchemaGenerationSupported(adp)) {
            logger.info {
                "Schema generation skipped for ADP '${adp.name}' (${adp.id}): connector has no specificationUri"
            }
            return
        }
        val schema = generateSchema(adp)
        adp.applySchema(schema)
        aggregatedDataProfileRepository.save(adp)
        logger.info { "Schema generated for ADP '${adp.name}' (${adp.id})" }
    }

    fun generateSchema(adp: AggregatedDataProfile): String {
        val adpMock = generateConnectorMock(adp.connectorInstanceId, adp.connectorEndpointId)
        val composedInput = composeInput(adpMock, adp.level1Relations())
        val result = applyTransform(adp.resultTransform.expression, composedInput)
        return jsonSchemaInferrer.infer(result)
    }

    fun isSchemaGenerationSupported(adp: AggregatedDataProfile): Boolean =
        (listOf(adp.connectorInstanceId) + adp.relations.map { it.connectorInstanceId })
            .all { instanceId ->
                connectorInstanceRepository.findById(instanceId)
                    .map { it.config.containsKey("specificationUri") }
                    .orElse(false)
            }

    private fun generateRelationResult(relation: Relation): JsonNode {
        val connectorMock = generateConnectorMock(relation.connectorInstanceId, relation.connectorEndpointId)
        val children = relation.aggregatedDataProfile.relationsOf(relation.id)
        val composedInput = composeInput(connectorMock, children)
        return applyTransform(relation.resultTransform.expression, composedInput)
    }

    private fun composeInput(
        connectorMock: JsonNode,
        children: List<Relation>,
    ): JsonNode {
        if (children.isEmpty()) return connectorMock

        val rightMap = mapper.createObjectNode().apply {
            children.forEach { child ->
                val childResult = generateRelationResult(child)
                val isArrayMode = detectArrayMode(child.endpointTransform.expression, connectorMock)
                if (isArrayMode) {
                    set<ArrayNode>(child.propertyName, mapper.createArrayNode().add(childResult))
                } else {
                    set<JsonNode>(child.propertyName, childResult)
                }
            }
        }

        return mapper.createObjectNode().apply {
            set<ObjectNode>("left", connectorMock)
            set<ObjectNode>("right", rightMap)
        }
    }

    private fun generateConnectorMock(connectorInstanceId: UUID, connectorEndpointId: UUID): JsonNode {
        val instance = connectorInstanceRepository.findById(connectorInstanceId)
            .orElseThrow { NoSuchElementException("ConnectorInstance not found: $connectorInstanceId") }
        val endpoint = connectorEndpointRepository.findById(connectorEndpointId)
            .orElseThrow { NoSuchElementException("ConnectorEndpoint not found: $connectorEndpointId") }
        val specUri = checkNotNull(instance.config["specificationUri"]) {
            "specificationUri missing from ConnectorInstance ${instance.id}"
        }
        return openApiMockGenerator.loadSpec(specUri).let { openApi ->
            openApiMockGenerator.generateMock(openApi, endpoint.operation)
        }
    }

    private fun detectArrayMode(endpointTransformExpression: String, parentMock: JsonNode): Boolean {
        val mockContext = mapper.createObjectNode().apply {
            put("idParam", "")
            set<ObjectNode>("sortParams", mapper.createObjectNode())
            set<ObjectNode>("filterParams", mapper.createObjectNode())
            set<JsonNode>("source", parentMock)
        }
        return try {
            val result = applyTransform(endpointTransformExpression, mockContext)
            result.isArray
        } catch (e: Exception) {
            false
        }
    }

    private fun applyTransform(expression: String, input: JsonNode): JsonNode {
        val scope = Scope.newEmptyScope().also {
            BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, it)
        }
        val query = JsonQuery.compile(expression, Versions.JQ_1_6)
        return buildList<JsonNode> { query.apply(scope, input) { add(it) } }
            .firstOrNull() ?: NullNode.instance
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}