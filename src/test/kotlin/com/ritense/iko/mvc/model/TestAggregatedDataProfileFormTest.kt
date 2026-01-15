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