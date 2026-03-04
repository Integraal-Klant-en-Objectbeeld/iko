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
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.OpenAPIV3Parser
import org.springframework.stereotype.Component

@Component
internal class OpenApiMockGenerator(
    private val mapper: ObjectMapper,
) {

    fun loadSpec(specUri: String): OpenAPI = when {
        specUri.startsWith("classpath:") -> {
            val path = specUri.removePrefix("classpath:")
            val content = javaClass.classLoader.getResourceAsStream(path)
                ?.bufferedReader()?.readText()
                ?: error("Classpath resource not found: $path")
            OpenAPIV3Parser().readContents(content).openAPI
                ?: error("Failed to parse OpenAPI spec from classpath: $path")
        }
        else -> OpenAPIV3Parser().read(specUri)
            ?: error("Failed to fetch or parse OpenAPI spec from: $specUri")
    }

    fun generateMock(openApi: OpenAPI, operationId: String): JsonNode {
        val schema = findResponseSchema(openApi, operationId)
            ?: return NullNode.instance
        return generateNode(schema, openApi, depth = 0)
    }

    private fun findResponseSchema(openApi: OpenAPI, operationId: String): Schema<*>? {
        for (pathItem in openApi.paths?.values ?: emptyList()) {
            val operation = listOfNotNull(
                pathItem.get,
                pathItem.post,
                pathItem.put,
                pathItem.patch,
                pathItem.delete,
            ).firstOrNull { it.operationId == operationId } ?: continue

            val response = operation.responses
                ?.entries
                ?.firstOrNull { (code, _) -> code == "200" || code.startsWith("2") }
                ?.value ?: continue

            return response.content
                ?.entries
                ?.firstOrNull { (mediaType, _) -> mediaType.contains("json") }
                ?.value?.schema
        }
        return null
    }

    private fun generateNode(
        schema: Schema<*>,
        openApi: OpenAPI,
        depth: Int,
        visited: MutableSet<Schema<*>> = mutableSetOf(),
    ): JsonNode {
        if (depth > 20) return NullNode.instance

        schema.`$ref`?.let { ref ->
            val refName = ref.substringAfterLast("/")
            val resolved = openApi.components?.schemas?.get(refName) ?: return NullNode.instance
            return generateNode(resolved, openApi, depth + 1, visited)
        }

        if (schema !in visited) {
            schema.discriminator?.mapping?.values?.firstOrNull()?.let { ref ->
                visited.add(schema)
                val refName = ref.substringAfterLast("/")
                val resolved = openApi.components?.schemas?.get(refName) ?: return@let
                return generateNode(resolved, openApi, depth + 1, visited)
            }
        }

        val composites = listOfNotNull(schema.allOf, schema.anyOf, schema.oneOf).flatten()
        if (composites.isNotEmpty()) {
            val results = composites.map { sub -> generateNode(sub, openApi, depth + 1, visited) }
            val objectResults = results.filter { it.isObject }

            if (objectResults.isNotEmpty() || !schema.properties.isNullOrEmpty()) {
                val merged = mapper.createObjectNode()
                objectResults.forEach { merged.setAll<ObjectNode>(it as ObjectNode) }
                if (!schema.properties.isNullOrEmpty()) {
                    val direct = generateByType(schema, openApi, depth, visited)
                    if (direct.isObject) merged.setAll<ObjectNode>(direct as ObjectNode)
                }
                return if (merged.size() > 0) merged else NullNode.instance
            }

            return results.firstOrNull { !it.isNull } ?: NullNode.instance
        }

        return generateByType(schema, openApi, depth, visited)
    }

    private fun resolveType(schema: Schema<*>): String? = schema.type ?: schema.types?.firstOrNull { it != "null" } ?: schema.types?.firstOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun generateByType(
        schema: Schema<*>,
        openApi: OpenAPI,
        depth: Int,
        visited: MutableSet<Schema<*>> = mutableSetOf(),
    ): JsonNode = when (resolveType(schema)) {
        "object" -> {
            val obj = mapper.createObjectNode()
            schema.properties?.forEach { (key, propSchema) ->
                obj.set<JsonNode>(key, generateNode(propSchema as Schema<*>, openApi, depth + 1, visited))
            }
            obj
        }
        "array" -> {
            val arr = mapper.createArrayNode()
            schema.items?.let { arr.add(generateNode(it as Schema<*>, openApi, depth + 1, visited)) }
            arr
        }
        "string" -> TextNode.valueOf(
            schema.example?.toString() ?: schema.enum?.firstOrNull()?.toString() ?: "example",
        )
        "integer" -> IntNode.valueOf((schema.example as? Int) ?: 0)
        "number" -> DoubleNode.valueOf((schema.example as? Number)?.toDouble() ?: 0.0)
        "boolean" -> BooleanNode.FALSE
        null -> {
            if (!schema.properties.isNullOrEmpty()) {
                generateByType(
                    schema.apply { type = "object" },
                    openApi,
                    depth,
                    visited,
                )
            } else {
                NullNode.instance
            }
        }
        else -> NullNode.instance
    }
}