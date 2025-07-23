package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.UniqueName
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank

data class AddProfileForm(
    @field:NotBlank(message = "Please provide a name.")
    @field:UniqueName
    val name: String,
    @field:NotBlank(message = "Please select a primary endpoint.")
    val primaryEndpoint: String,
    @field:NotBlank(message = "Please provide a transform.")
    @field:ValidTransform
    var transform: String
)
