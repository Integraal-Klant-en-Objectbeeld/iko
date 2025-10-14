package com.ritense.iko.mvc.model.connector

import jakarta.validation.constraints.NotBlank

data class ConnectorCreateForm(
    @field:NotBlank(message = "Please provide a name.")
    val name: String,
    @field:NotBlank(message = "Please provide a reference.")
    val reference: String,
    @field:NotBlank(message = "Please provide a connector code.")
    val connectorCode: String,
)