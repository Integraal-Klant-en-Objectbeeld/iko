/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
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

package com.ritense.iko.aggregateddataprofile

import com.ritense.iko.aggregateddataprofile.domain.Transform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TransformTest {
    @Test
    fun `should create valid Transform class`() {
        val transform = Transform("test")
        assertThat(transform.expression).isEqualTo("test")
    }
}