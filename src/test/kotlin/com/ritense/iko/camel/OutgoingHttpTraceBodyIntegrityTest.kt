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

package com.ritense.iko.camel

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.ritense.iko.camel.IkoConstants.Headers.IKO_TRACE_SPAN_HEADER
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.http.HttpComponent
import org.apache.camel.impl.DefaultCamelContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Sends real requests through camel-http (and rest-openapi, as connectors do) with the debug-trace interceptor
 * installed, and asserts on what the server actually received. Guards against the capture consuming a
 * streaming request body (a jq-built JsonNode), which made debug runs send an empty body upstream.
 */
class OutgoingHttpTraceBodyIntegrityTest {

    private val server = MockWebServer()
    private val registry = OutgoingHttpTraceRegistry()
    private val camelContext = DefaultCamelContext()

    @BeforeEach
    fun setUp() {
        server.start()
        camelContext.addComponent("http", HttpComponent())
        HttpTraceConfiguration(camelContext, registry).register()
        val spec = Paths.get("openapi-specs/haalcentraal-brp-personen.yaml").toAbsolutePath()
        camelContext.addRoutes(
            object : RouteBuilder() {
                override fun configure() {
                    from("direct:http")
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                        .to("http://localhost:${server.port}/x")
                    from("direct:rest-openapi")
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .toD("rest-openapi:file:$spec#Personen?host=http://localhost:${server.port}")
                }
            },
        )
        camelContext.start()
    }

    @AfterEach
    fun tearDown() {
        camelContext.stop()
        server.shutdown()
    }

    @Test
    fun `json node body is sent intact during a debug run`() {
        send("direct:http", jsonBody(), traced = true)

        val received = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertThat(received.body.readUtf8()).isEqualTo(JSON)
        assertThat(received.getHeader(IKO_TRACE_SPAN_HEADER)).isNull()
        assertThat(registry.drain(TRACE_ID).single().body).isNull()
    }

    @Test
    fun `json node body is sent intact through rest-openapi during a debug run`() {
        send("direct:rest-openapi", jsonBody(), traced = true)

        val received = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertThat(received.path).isEqualTo("/haalcentraal/api/brp/personen")
        assertThat(received.body.readUtf8()).isEqualTo(JSON)
        assertThat(received.getHeader(IKO_TRACE_SPAN_HEADER)).isNull()
    }

    @Test
    fun `string body is sent intact and captured during a debug run`() {
        send("direct:http", JSON, traced = true)

        val received = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertThat(received.body.readUtf8()).isEqualTo(JSON)
        assertThat(registry.drain(TRACE_ID).single().body).isEqualTo(JSON)
    }

    @Test
    fun `json node body is sent intact and nothing is captured outside a debug run`() {
        send("direct:http", jsonBody(), traced = false)

        val received = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertThat(received.body.readUtf8()).isEqualTo(JSON)
        assertThat(registry.drain(TRACE_ID)).isEmpty()
    }

    private fun send(endpoint: String, body: Any, traced: Boolean) {
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody("{}"))
        camelContext.createProducerTemplate().send(endpoint) { exchange ->
            exchange.message.body = body
            if (traced) {
                exchange.message.setHeader(IKO_TRACE_SPAN_HEADER, TraceSpan.encode(TRACE_ID, exchange.exchangeId))
            }
        }.also { assertThat(it.exception).isNull() }
    }

    private fun jsonBody() = JsonNodeFactory.instance.objectNode().put("a", 1)

    companion object {
        private const val TRACE_ID = "T1"
        private const val JSON = """{"a":1}"""
    }
}