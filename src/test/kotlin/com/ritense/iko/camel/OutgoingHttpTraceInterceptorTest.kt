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

import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.message.BasicClassicHttpRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OutgoingHttpTraceInterceptorTest {

    private val registry = OutgoingHttpTraceRegistry()
    private val interceptor = OutgoingHttpTraceInterceptor(registry)

    @Test
    fun `captures the resolved request keyed by the trace-span header`() {
        val request = BasicClassicHttpRequest("POST", "http://host/zaken?zaak=1")
        request.addHeader("iko_trace_span", "T1|E1")
        request.addHeader("Accept", "application/json")
        request.entity = StringEntity("""{"q":"x"}""")

        interceptor.process(request, request.entity, null)

        val captured = registry.drain("T1")
        assertThat(captured).hasSize(1)
        val capture = captured.single()
        assertThat(capture.spanId).isEqualTo("E1")
        assertThat(capture.method).isEqualTo("POST")
        assertThat(capture.uri).contains("/zaken?zaak=1")
        assertThat(capture.body).isEqualTo("""{"q":"x"}""")
        assertThat(capture.headers).containsEntry("Accept", "application/json")
        // The internal correlation header is stripped from the displayed request headers.
        assertThat(capture.headers).doesNotContainKey("iko_trace_span")
    }

    @Test
    fun `ignores requests without a trace-span header (production traffic)`() {
        val request = BasicClassicHttpRequest("GET", "http://host/zaken")

        interceptor.process(request, null, null)

        assertThat(registry.drain("T1")).isEmpty()
    }
}