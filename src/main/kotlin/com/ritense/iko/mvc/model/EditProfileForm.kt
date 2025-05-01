package com.ritense.iko.mvc.model

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class EditProfileForm(
    val id: UUID,
    @field:NotBlank
    val name: String,
    // val primarySource: String,
    @field:NotBlank
    var transform: String // Custom validator
)
