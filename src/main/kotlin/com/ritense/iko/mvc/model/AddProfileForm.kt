package com.ritense.iko.mvc.model

import jakarta.validation.constraints.NotBlank

data class AddProfileForm(
    @field:NotBlank(message = "Please provide a name.")
    val name: String,
    @field:NotBlank(message = "Please select a primary search.")
    val primarySearch: String,
    @field:NotBlank(message = "Please provide a transform.")
    @field:ValidTransform
    var transform: String
)
