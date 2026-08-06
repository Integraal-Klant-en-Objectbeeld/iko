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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TraceSpanTest {

    @Test
    fun `encode and decode round-trip`() {
        val encoded = TraceSpan.encode("trace-1", "EX-42")
        assertThat(encoded).isEqualTo("trace-1|EX-42")
        assertThat(TraceSpan.decode(encoded)).isEqualTo("trace-1" to "EX-42")
    }

    @Test
    fun `decode returns null for a malformed header`() {
        assertThat(TraceSpan.decode("no-separator")).isNull()
        assertThat(TraceSpan.decode("|only-span")).isNull()
    }
}