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
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component

@Component
internal class JsonSchemaInferrer(
    private val mapper: ObjectMapper,
) {

    fun infer(root: JsonNode): String {
        val schema = inferShape(root)
        schema.put("\$schema", "https://json-schema.org/draft/2020-12/schema")
        return mapper.writeValueAsString(schema)
    }

    private fun inferShape(node: JsonNode): ObjectNode {
        val schema = mapper.createObjectNode()
        return when {
            node.isObject -> {
                schema.put("type", "object")
                if (node.size() > 0) {
                    val props = mapper.createObjectNode()
                    node.properties().forEach { (key, value) ->
                        props.set<ObjectNode>(key, inferShape(value))
                    }
                    schema.set<ObjectNode>("properties", props)
                }
                schema
            }
            node.isArray -> {
                schema.put("type", "array")
                if (node.size() > 0) schema.set<ObjectNode>("items", inferShape(node.first()))
                schema
            }
            node.isTextual -> schema.apply { put("type", "string") }
            node.isInt -> schema.apply { put("type", "integer") }
            node.isNumber -> schema.apply { put("type", "number") }
            node.isBoolean -> schema.apply { put("type", "boolean") }
            else -> schema.apply { put("type", "null") }
        }
    }
}