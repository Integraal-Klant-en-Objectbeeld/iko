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

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A single fully-resolved outgoing HTTP request captured at the Apache HttpClient layer during a debug
 * trace (rest-openapi builds and sends this internally, so it never surfaces on the Camel exchange).
 * [spanId] is the Camel exchange id used to map the capture back to its trace step.
 */
data class OutgoingHttpCapture(
    val spanId: String?,
    val method: String?,
    val uri: String?,
    val headers: Map<String, String>,
    val body: String?,
)

/**
 * Trace-only sink for the real outgoing HTTP requests IKO makes. The request interceptor records here keyed
 * by the debug run's `iko_trace_id`; [com.ritense.iko.mvc.controller.TestController] drains the run's
 * captures and hands them to the trace graph builder. Never populated outside a debug run.
 */
@Component
internal class OutgoingHttpTraceRegistry {
    private val byTrace = ConcurrentHashMap<String, CopyOnWriteArrayList<OutgoingHttpCapture>>()

    fun record(traceId: String, capture: OutgoingHttpCapture) {
        val list = byTrace.computeIfAbsent(traceId) { CopyOnWriteArrayList() }
        if (list.size < MAX_PER_TRACE) list.add(capture)
    }

    fun drain(traceId: String): List<OutgoingHttpCapture> = byTrace.remove(traceId) ?: emptyList()

    companion object {
        private const val MAX_PER_TRACE = 100
    }
}