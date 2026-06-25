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

package com.ritense.iko.connectors.camel

import com.fasterxml.jackson.databind.JsonNode
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_OPERATION_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_TAG_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_VERSION_VARIABLE
import com.ritense.iko.camel.IkoRouteHelper.Companion.GLOBAL_ERROR_HANDLER_CONFIGURATION
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.builder.RouteConfigurationBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransformDispatcherRouteBuilderTest {

    private lateinit var context: DefaultCamelContext

    @BeforeEach
    fun setUp() {
        context = DefaultCamelContext()
        // The dispatcher references this route configuration; declare an empty one for the test.
        context.addRoutesConfigurations(
            object : RouteConfigurationBuilder() {
                override fun configuration() {
                    routeConfiguration(GLOBAL_ERROR_HANDLER_CONFIGURATION)
                }
            },
        )
        context.addRoutes(TransformDispatcherRouteBuilder())
        // Stand-in for a connector-supplied endpoint transform route that builds its request
        // body from a header using a `setBody: jq:` step, mirroring the BRP connector.
        context.addRoutes(
            object : RouteBuilder() {
                override fun configure() {
                    from("direct:iko:endpoint:transform:test:1.0.0.Personen")
                        .routeId("direct:iko:endpoint:transform:test:1.0.0.Personen")
                        .setBody().jq("""{ burgerservicenummer: header("burgerservicenummer") }""")
                }
            },
        )
        context.start()
    }

    @AfterEach
    fun tearDown() {
        context.stop()
    }

    @Test
    fun `seeds an empty json body so jq setBody works with a null body`() {
        val result = context.createProducerTemplate().send("direct:iko:endpoint:transform") { exchange ->
            exchange.setVariable(CONNECTOR_TAG_VARIABLE, "test")
            exchange.setVariable(CONNECTOR_VERSION_VARIABLE, "1.0.0")
            exchange.setVariable(CONNECTOR_OPERATION_VARIABLE, "Personen")
            exchange.message.setHeader("burgerservicenummer", "999993653")
            exchange.message.body = null
        }

        assertThat(result.exception).isNull()
        val body = result.message.getBody(JsonNode::class.java)
        assertThat(body.get("burgerservicenummer").asText()).isEqualTo("999993653")
    }
}