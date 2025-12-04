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