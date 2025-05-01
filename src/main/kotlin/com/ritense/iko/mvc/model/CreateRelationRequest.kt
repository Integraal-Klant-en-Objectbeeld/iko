package com.ritense.iko.mvc.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateRelationRequest(
    @NotNull(message = "Profile ID cannot be null")
    val profileId: UUID,

    @NotBlank(message = "Name cannot be empty")
    val name: String,
    // val primarySource: String,

    @NotBlank(message = "Transform cannot be empty")
    var transform: String
)
