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
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.Exchange
import org.apache.camel.Processor

/**
 * Captures a JSON snapshot of the current exchange body into the [slot] variable, but **only during a debug
 * run** — guarded by the `iko_trace_id` variable, exactly like the HTTP-body capture in [ErrorHelper] — so
 * it adds no cost to normal API traffic.
 *
 * At the aggregation / result boundaries the body is an in-memory `Map`/`JsonNode`, which the BacklogTracer
 * captures via `toString()` — a lossy `key=value` dump that is not JSON and cannot be safely parsed back
 * (real values contain `,`/`:`/`=`). Stashing a faithful JSON copy here lets the trace viewer pretty-print
 * it. A serialization failure never affects the run: the body passes through untouched and the slot is left
 * unset (the viewer falls back to the raw traced body).
 */
internal class TraceBodyJsonProcessor(
    private val objectMapper: ObjectMapper,
    private val slot: String,
) : Processor {

    override fun process(exchange: Exchange) {
        if (exchange.getVariable(IKO_TRACE_ID_VARIABLE, String::class.java) == null) return
        val body = exchange.message.body ?: return
        runCatching { objectMapper.writeValueAsString(body) }
            .onSuccess { exchange.setVariable(slot, it) }
            .onFailure { logger.debug(it) { "Could not capture trace body JSON for slot=$slot" } }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}