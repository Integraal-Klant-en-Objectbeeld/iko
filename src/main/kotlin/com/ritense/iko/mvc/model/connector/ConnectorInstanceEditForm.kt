package com.ritense.iko.mvc.model.connector

import jakarta.validation.constraints.NotBlank

class ConnectorInstanceEditForm(
    @field:NotBlank(message = "Please provide a name.")
    val name: String,
    @field:NotBlank(message = "Please provide a reference.")
    val reference: String,
)