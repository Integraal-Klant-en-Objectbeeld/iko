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

/**
 * Wire format of the `iko_trace_span` header: `<traceId>|<exchangeId>`. The trace id keys the outgoing-http
 * registry; the exchange id correlates the captured request to its trace step.
 */
internal object TraceSpan {
    private const val SEPARATOR = '|'

    fun encode(traceId: String, exchangeId: String): String = "$traceId$SEPARATOR$exchangeId"

    fun decode(header: String): Pair<String, String>? {
        val i = header.indexOf(SEPARATOR)
        if (i <= 0) return null
        return header.substring(0, i) to header.substring(i + 1)
    }
}