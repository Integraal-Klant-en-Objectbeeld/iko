package com.ritense.iko.mvc.connector

import jakarta.validation.constraints.NotBlank

class ConnectorInstanceConfigEditForm(
    @field:NotBlank(message = "Please provide a key.")
    val key: String,
    @field:NotBlank(message = "Please provide a value.")
    val value: String,
)
