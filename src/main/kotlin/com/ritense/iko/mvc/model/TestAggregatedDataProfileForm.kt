package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class TestAggregatedDataProfileForm(
    val id: UUID,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val transform: String,
    val testId: String,
    val name: String,
)

