package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank

data class TestAggregatedDataProfileForm(
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val transform: String,
    val testId: String,
    val name: String,
)