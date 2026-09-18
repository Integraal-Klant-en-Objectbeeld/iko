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

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.camel.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TraceBodyJsonProcessorTest {

    private val camelContext = DefaultCamelContext()
    private val objectMapper = ObjectMapper()

    private fun exchange(traceId: String?, body: Any?): Exchange = DefaultExchange(camelContext).also {
        it.message.body = body
        if (traceId != null) it.setVariable(IKO_TRACE_ID_VARIABLE, traceId)
    }

    @Test
    fun `captures the body as json into the slot during a trace run`() {
        val exchange = exchange(traceId = "t1", body = linkedMapOf("a" to 1, "b" to "x"))

        TraceBodyJsonProcessor(objectMapper, "slot").process(exchange)

        assertThat(exchange.getVariable("slot", String::class.java)).isEqualTo("{\"a\":1,\"b\":\"x\"}")
    }

    @Test
    fun `does nothing when there is no trace id (normal traffic)`() {
        val exchange = exchange(traceId = null, body = linkedMapOf("a" to 1))

        TraceBodyJsonProcessor(objectMapper, "slot").process(exchange)

        assertThat(exchange.getVariable("slot", String::class.java)).isNull()
    }

    @Test
    fun `swallows serialization failures without affecting the run`() {
        val failing = mock<ObjectMapper>()
        whenever(failing.writeValueAsString(any())).doThrow(RuntimeException("boom"))
        val exchange = exchange(traceId = "t1", body = linkedMapOf("a" to 1))

        TraceBodyJsonProcessor(failing, "slot").process(exchange)

        assertThat(exchange.getVariable("slot", String::class.java)).isNull()
        assertThat(exchange.message.body).isEqualTo(linkedMapOf("a" to 1))
    }
}