package com.ritense.iko.mvc.connector

import jakarta.validation.constraints.NotBlank

data class ConnectorEditForm(
    @field:NotBlank(message = "Please provide a connector code.")
    val connectorCode: String,
)