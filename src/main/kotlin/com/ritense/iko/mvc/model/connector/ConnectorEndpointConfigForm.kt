package com.ritense.iko.mvc.model.connector

import jakarta.validation.constraints.NotBlank

data class ConnectorEndpointConfigForm(
    @field:NotBlank(message = "Please provide a name.")
    val name: String,
    @field:NotBlank(message = "Please provide an operation.")
    val operation: String,
)