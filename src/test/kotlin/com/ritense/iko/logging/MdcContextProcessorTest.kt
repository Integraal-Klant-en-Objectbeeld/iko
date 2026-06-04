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

package com.ritense.iko.logging

import com.ritense.iko.camel.IkoConstants.Variables.IKO_CORRELATION_ID_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import org.apache.camel.Exchange
import org.apache.camel.ExchangeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.MDC

internal class MdcContextProcessorTest {

    private val processor = MdcContextProcessor()

    @AfterEach
    fun clearMdc() {
        MDC.remove(IKO_CORRELATION_ID_VARIABLE)
        MDC.remove(IKO_TRACE_ID_VARIABLE)
    }

    private fun mockExchange(): Exchange {
        val exchange = mock<Exchange>()
        val exchangeExtension = mock<ExchangeExtension>()
        whenever(exchange.exchangeExtension).thenReturn(exchangeExtension)
        return exchange
    }

    @Test
    fun `process puts correlationId into MDC when variable is set`() {
        val exchange = mockExchange()
        whenever(exchange.getVariable(IKO_CORRELATION_ID_VARIABLE, String::class.java)).thenReturn("test-correlation-id")
        whenever(exchange.getVariable(IKO_TRACE_ID_VARIABLE, String::class.java)).thenReturn(null)

        processor.process(exchange)

        assertThat(MDC.get(IKO_CORRELATION_ID_VARIABLE))
            .isEqualTo("test-correlation-id")
        assertThat(MDC.get(IKO_TRACE_ID_VARIABLE))
            .isNull()
    }

    @Test
    fun `process puts both correlationId and iko_trace_id into MDC when both variables are set`() {
        val exchange = mockExchange()
        whenever(exchange.getVariable(IKO_CORRELATION_ID_VARIABLE, String::class.java)).thenReturn("corr-123")
        whenever(exchange.getVariable(IKO_TRACE_ID_VARIABLE, String::class.java)).thenReturn("trace-456")

        processor.process(exchange)

        assertThat(MDC.get(IKO_CORRELATION_ID_VARIABLE)).isEqualTo("corr-123")
        assertThat(MDC.get(IKO_TRACE_ID_VARIABLE)).isEqualTo("trace-456")
    }

    @Test
    fun `process does not put null variables into MDC`() {
        val exchange = mockExchange()
        whenever(exchange.getVariable(IKO_CORRELATION_ID_VARIABLE, String::class.java)).thenReturn(null)
        whenever(exchange.getVariable(IKO_TRACE_ID_VARIABLE, String::class.java)).thenReturn(null)

        processor.process(exchange)

        assertThat(MDC.get(IKO_CORRELATION_ID_VARIABLE)).isNull()
        assertThat(MDC.get(IKO_TRACE_ID_VARIABLE)).isNull()
    }
}