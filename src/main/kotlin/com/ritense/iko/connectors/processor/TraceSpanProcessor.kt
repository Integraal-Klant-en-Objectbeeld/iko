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

package com.ritense.iko.connectors.processor

import com.ritense.iko.camel.IkoConstants.Headers.IKO_TRACE_SPAN_HEADER
import com.ritense.iko.camel.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import com.ritense.iko.camel.TraceSpan
import org.apache.camel.Exchange
import org.apache.camel.Processor

/**
 * Stamps the `iko_trace_span` header before an outgoing connector call, only during a debug run. The header
 * survives a connector's `removeHeaders` (unlike the `iko_trace_id` exchange header) and is read back by the
 * [com.ritense.iko.camel.OutgoingHttpTraceInterceptor].
 */
internal class TraceSpanProcessor : Processor {
    override fun process(exchange: Exchange) {
        val traceId = exchange.getVariable(IKO_TRACE_ID_VARIABLE, String::class.java) ?: return
        exchange.message.setHeader(IKO_TRACE_SPAN_HEADER, TraceSpan.encode(traceId, exchange.exchangeId))
    }
}