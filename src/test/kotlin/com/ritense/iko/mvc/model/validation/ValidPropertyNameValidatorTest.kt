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

package com.ritense.iko.mvc.model.validation

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ValidPropertyNameValidatorTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    private data class Subject(
        @field:ValidPropertyName
        val propertyName: String?,
    )

    @ParameterizedTest
    @ValueSource(strings = ["owner", "pet_owner", "PetOwner", "pet1", "_hidden", "a", "ABC_123"])
    fun `valid property names pass`(name: String) {
        val violations = validator.validate(Subject(name))
        assertThat(violations).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["pet-owner", "pet owner", "pet.owner", "pet/owner", "pet@owner", "ünïcödé"])
    fun `property names with invalid characters fail`(name: String) {
        val violations = validator.validate(Subject(name))
        assertThat(violations).hasSize(1)
        val violation = violations.single()
        assertThat(violation.propertyPath.toString()).isEqualTo("propertyName")
        assertThat(violation.message).contains(name)
        assertThat(violation.message).contains("a-z, A-Z, 0-9, and _")
    }

    @Test
    fun `null is treated as valid — delegated to NotBlank`() {
        val violations = validator.validate(Subject(null))
        assertThat(violations).isEmpty()
    }

    @Test
    fun `blank string is treated as valid — delegated to NotBlank`() {
        val violations = validator.validate(Subject(""))
        assertThat(violations).isEmpty()
    }

    @Test
    fun `whitespace-only string is treated as valid — delegated to NotBlank`() {
        val violations = validator.validate(Subject("   "))
        assertThat(violations).isEmpty()
    }
}