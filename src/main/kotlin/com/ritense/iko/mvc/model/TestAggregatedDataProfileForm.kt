package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank

data class TestAggregatedDataProfileForm(
    @field:NotBlank(message = "Please provide a valid json object.")
    val endpointTransformContext: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val resultTransform: String,
    val name: String,
)