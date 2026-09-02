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

package com.ritense.iko.mvc.model.trace

import org.springframework.stereotype.Component

/**
 * Masks every header value with `***` unless [DebugTraceProperties.showHeaders] is enabled, so raw
 * credentials never reach the browser. Applied to every header map the trace viewer emits.
 */
@Component
internal class HeaderRedactor(private val properties: DebugTraceProperties) {

    fun redact(headers: Map<String, String>): Map<String, String> = if (properties.showHeaders) headers else headers.mapValues { MASK }

    companion object {
        private const val MASK = "***"
    }
}