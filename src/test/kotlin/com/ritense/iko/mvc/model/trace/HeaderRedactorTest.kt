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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HeaderRedactorTest {

    @Test
    fun `masks every header value by default`() {
        val redactor = HeaderRedactor(DebugTraceProperties())

        val result = redactor.redact(mapOf("Content-Type" to "application/json", "Authorization" to "Bearer secret"))

        assertThat(result["Content-Type"]).isEqualTo("***")
        assertThat(result["Authorization"]).isEqualTo("***")
    }

    @Test
    fun `shows all header values when show-headers is enabled`() {
        val redactor = HeaderRedactor(DebugTraceProperties(showHeaders = true))

        val result = redactor.redact(mapOf("Content-Type" to "application/json", "Authorization" to "Bearer secret"))

        assertThat(result["Content-Type"]).isEqualTo("application/json")
        assertThat(result["Authorization"]).isEqualTo("Bearer secret")
    }

    @Test
    fun `leaves an empty map untouched`() {
        assertThat(HeaderRedactor(DebugTraceProperties()).redact(emptyMap())).isEmpty()
    }
}