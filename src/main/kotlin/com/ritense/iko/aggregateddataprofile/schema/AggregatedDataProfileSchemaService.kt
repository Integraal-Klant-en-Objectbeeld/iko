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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
internal class AggregatedDataProfileSchemaService(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val openApiMockGenerator: OpenApiMockGenerator,
    private val jsonSchemaInferrer: JsonSchemaInferrer,
    private val mapper: ObjectMapper,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @Transactional
    fun generateAndSave(adpId: UUID) {
        val adp = aggregatedDataProfileRepository.findById(adpId)
            .orElseThrow { NoSuchElementException("ADP not found: $adpId") }
        val schema = generateSchema(adp)
        if (schema == null) {
            logger.info {
                "Schema generation skipped for ADP '${adp.name}' (${adp.id}): connector has no specificationUri"
            }
            return
        }
        adp.jsonschema = schema
        aggregatedDataProfileRepository.save(adp)
        logger.info { "Schema generated for ADP '${adp.name}' (${adp.id})" }
    }

    fun generateSchema(adp: AggregatedDataProfile): String? {
        if (!isSchemaGenerationSupported(adp)) return null
        val adpMock = generateConnectorMock(adp.connectorInstanceId, adp.connectorEndpointId)
        val composedInput = composeInput(adpMock, adp.level1Relations(), adp)
        val result = applyTransform(adp.resultTransform.expression, composedInput)
        return jsonSchemaInferrer.infer(result)
    }

    private fun isSchemaGenerationSupported(adp: AggregatedDataProfile): Boolean {
        val allInstanceIds = buildList {
            add(adp.connectorInstanceId)
            adp.relations.forEach { add(it.connectorInstanceId) }
        }
        return allInstanceIds.all { instanceId ->
            connectorInstanceRepository.findById(instanceId)
                .map { it.config.containsKey("specificationUri") }
                .orElse(false)
        }
    }

    private fun generateRelationResult(relation: Relation): JsonNode {
        val connectorMock = generateConnectorMock(relation.connectorInstanceId, relation.connectorEndpointId)
        val children = relation.aggregatedDataProfile.relationsOf(relation.id)
        val composedInput = composeInput(connectorMock, children, relation.aggregatedDataProfile)
        return applyTransform(relation.resultTransform.expression, composedInput)
    }

    private fun composeInput(
        connectorMock: JsonNode,
        children: List<Relation>,
        adp: AggregatedDataProfile,
    ): JsonNode {
        if (children.isEmpty()) return connectorMock

        val rightMap = mapper.createObjectNode()
        for (child in children) {
            val childResult = generateRelationResult(child)
            val isArrayMode = detectArrayMode(child.endpointTransform.expression, connectorMock)
            if (isArrayMode) {
                rightMap.set<ArrayNode>(child.propertyName, mapper.createArrayNode().add(childResult))
            } else {
                rightMap.set<JsonNode>(child.propertyName, childResult)
            }
        }

        return mapper.createObjectNode()
            .set<ObjectNode>("left", connectorMock).also {
                (it as ObjectNode).set<ObjectNode>("right", rightMap)
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
        val openApi = openApiMockGenerator.loadSpec(specUri)
        return openApiMockGenerator.generateMock(openApi, endpoint.operation)
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
        val scope = Scope.newEmptyScope()
        BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, scope)
        val query = JsonQuery.compile(expression, Versions.JQ_1_6)
        val out = mutableListOf<JsonNode>()
        query.apply(scope, input) { out.add(it) }
        return out.firstOrNull() ?: NullNode.instance
    }
}