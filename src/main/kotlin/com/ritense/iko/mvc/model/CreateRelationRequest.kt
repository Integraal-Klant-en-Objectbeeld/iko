package com.ritense.iko.mvc.model

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateRelationRequest(
    val profileId: UUID,
    val sourceId: String,
    val searchId: String,
    val sourceToSearchMapping: String,
    @NotBlank(message = "Transform cannot be blank")
    var transform: String
)
