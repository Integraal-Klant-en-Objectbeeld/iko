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

package com.ritense.iko.mvc.model

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestAggregatedDataProfileFormTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `valid form passes validation`() {
        val form = TestAggregatedDataProfileForm(
            endpointTransformContext = "{}",
            resultTransform = ".",
            name = "pets",
        )

        val violations = validator.validate(form)

        assertThat(violations).isEmpty()
    }

    @Test
    fun `blank required fields fail validation`() {
        val form = TestAggregatedDataProfileForm(
            endpointTransformContext = " ",
            resultTransform = "",
            name = "pets",
        )

        val violations = validator.validate(form)

        val properties = violations.map { it.propertyPath.toString() }
        assertThat(properties).contains("endpointTransformContext", "resultTransform")
        assertThat(violations.map { it.message }).anyMatch { it.contains("Please provide", ignoreCase = true) }
    }
}