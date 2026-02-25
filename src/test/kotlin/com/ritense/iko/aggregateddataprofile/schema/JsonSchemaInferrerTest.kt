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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JsonSchemaInferrerTest {

    private val mapper = ObjectMapper()
    private val inferrer = JsonSchemaInferrer(mapper)

    @Test
    fun `infer produces object schema with properties`() {
        val input = mapper.readTree("""{"name": "test", "age": 42}""")

        val schema = mapper.readTree(inferrer.infer(input))

        assertThat(schema.get("\$schema").asText()).isEqualTo("https://json-schema.org/draft/2020-12/schema")
        assertThat(schema.get("type").asText()).isEqualTo("object")
        assertThat(schema.get("properties").has("name")).isTrue()
        assertThat(schema.get("properties").get("name").get("type").asText()).isEqualTo("string")
        assertThat(schema.get("properties").get("age").get("type").asText()).isEqualTo("integer")
    }

    @Test
    fun `infer produces array schema with items`() {
        val input = mapper.readTree("""[{"id": 1}]""")

        val schema = mapper.readTree(inferrer.infer(input))

        assertThat(schema.get("type").asText()).isEqualTo("array")
        assertThat(schema.get("items").get("type").asText()).isEqualTo("object")
        assertThat(schema.get("items").get("properties").get("id").get("type").asText()).isEqualTo("integer")
    }

    @Test
    fun `infer handles string value`() {
        val input = mapper.readTree(""""hello"""")

        val schema = mapper.readTree(inferrer.infer(input))

        assertThat(schema.get("type").asText()).isEqualTo("string")
    }

    @Test
    fun `infer handles boolean value`() {
        val input = mapper.readTree("true")

        val schema = mapper.readTree(inferrer.infer(input))

        assertThat(schema.get("type").asText()).isEqualTo("boolean")
    }

    @Test
    fun `infer handles number value`() {
        val input = mapper.readTree("3.14")

        val schema = mapper.readTree(inferrer.infer(input))

        assertThat(schema.get("type").asText()).isEqualTo("number")
    }

    @Test
    fun `infer handles null value`() {
        val schema = mapper.readTree(inferrer.infer(NullNode.instance))

        assertThat(schema.get("type").asText()).isEqualTo("null")
    }

    @Test
    fun `infer handles nested objects`() {
        val input = mapper.readTree("""{"outer": {"inner": "value"}}""")

        val schema = mapper.readTree(inferrer.infer(input))

        val outerProp = schema.get("properties").get("outer")
        assertThat(outerProp.get("type").asText()).isEqualTo("object")
        assertThat(outerProp.get("properties").get("inner").get("type").asText()).isEqualTo("string")
    }

    @Test
    fun `infer handles empty object`() {
        val input = mapper.readTree("""{}""")

        val schema = mapper.readTree(inferrer.infer(input))

        assertThat(schema.get("type").asText()).isEqualTo("object")
        assertThat(schema.has("properties")).isFalse()
    }

    @Test
    fun `infer handles empty array`() {
        val input = mapper.readTree("""[]""")

        val schema = mapper.readTree(inferrer.infer(input))

        assertThat(schema.get("type").asText()).isEqualTo("array")
        assertThat(schema.has("items")).isFalse()
    }
}