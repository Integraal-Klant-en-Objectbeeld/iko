package com.ritense.iko.mvc.model

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class AddRelationForm(
    val profileId: UUID,
    val sourceId: String?,
    @field:NotBlank(message = "Please select a endpoint.")
    val endpointId: String,
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToSearchMapping: String,
    @field:NotBlank(message = "Please define a transform expression.")
    var transform: String,
)
