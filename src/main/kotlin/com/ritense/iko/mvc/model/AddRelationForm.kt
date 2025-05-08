package com.ritense.iko.mvc.model

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class AddRelationForm(
    val profileId: UUID,
    val sourceId: String?,
    @field:NotBlank(message = "Please select a search.")
    val searchId: String,
    @field:NotBlank(message = "sourceToSearchMapping cannot be blank")
    val sourceToSearchMapping: String,
    @field:NotBlank(message = "Transform cannot be blank")
    var transform: String,
)
