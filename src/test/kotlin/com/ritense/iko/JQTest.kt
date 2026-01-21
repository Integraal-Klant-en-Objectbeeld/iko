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

package com.ritense.iko

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.TextNode
import net.thisptr.jackson.jq.BuiltinFunctionLoader
import net.thisptr.jackson.jq.JsonQuery
import net.thisptr.jackson.jq.Scope
import net.thisptr.jackson.jq.Versions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertFails

class JQTest {
    private val mapper = ObjectMapper()

    @Test
    fun `valid jq expression should compile and evaluate`() {
        val scope = Scope.newEmptyScope()
        BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, scope)

        val jqExpr = ".foo"

        assertDoesNotThrow("Expected jq expression to be valid") {
            val query = JsonQuery.compile(jqExpr, Versions.JQ_1_6)
            val input = mapper.readTree("""{"foo": "bar"}""")
            val out = mutableListOf<JsonNode>()
            query.apply(scope, input) {
                out.add(it)
            }
            assertThat(out).contains(TextNode("bar"))
        }
    }

    @Test
    fun `valid jq expression should not compile and evaluate`() {
        val scope = Scope.newEmptyScope()
        BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, scope)

        val invalidJqExpr = "?"

        assertFails(message = "Cannot compile query: ?") {
            JsonQuery.compile(invalidJqExpr, Versions.JQ_1_6)
        }
    }
}