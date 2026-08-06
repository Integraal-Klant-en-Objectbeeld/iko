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

import com.ritense.iko.camel.IkoConstants.Headers.IKO_TRACE_SPAN_HEADER
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.EntityDetails
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpRequestInterceptor
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.protocol.HttpContext

/**
 * Apache HttpClient request interceptor that captures the fully-resolved outgoing request (method, URL,
 * curated headers, body) rest-openapi builds — the request that actually leaves the pipe, which the Camel
 * exchange never carries. Records only when the request has the `iko_trace_span` header (set solely during a
 * debug run, *after* a connector's `removeHeaders`, so it survives header stripping unlike the exchange's
 * own `iko_trace_id` header). That header carries `<traceId>|<exchangeId>`: the trace id keys the registry,
 * the exchange id correlates the capture to its trace step. Stateless and thread-safe: the cached, shared
 * HttpClient invokes it concurrently.
 */
internal class OutgoingHttpTraceInterceptor(
    private val registry: OutgoingHttpTraceRegistry,
) : HttpRequestInterceptor {

    override fun process(request: HttpRequest, entity: EntityDetails?, context: HttpContext?) {
        val header = request.getFirstHeader(IKO_TRACE_SPAN_HEADER)?.value ?: return
        val (traceId, spanId) = TraceSpan.decode(header) ?: return
        val uri = runCatching { request.uri.toString() }.getOrNull() ?: request.requestUri
        val headers = request.headers
            .filterNot { it.name.equals(IKO_TRACE_SPAN_HEADER, ignoreCase = true) }
            .associate { it.name to (it.value ?: "") }
        val body = (request as? ClassicHttpRequest)?.entity
            ?.takeIf { it.isRepeatable }
            ?.let { runCatching { EntityUtils.toString(it) }.getOrNull() }
        registry.record(
            traceId,
            OutgoingHttpCapture(spanId = spanId, method = request.method, uri = uri, headers = headers, body = body),
        )
    }
}