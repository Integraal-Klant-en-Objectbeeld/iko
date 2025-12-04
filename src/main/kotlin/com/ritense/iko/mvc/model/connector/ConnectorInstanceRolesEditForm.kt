package com.ritense.iko.mvc.model.connector

import jakarta.validation.constraints.NotBlank

class ConnectorInstanceRolesEditForm(
    @field:NotBlank(message = "Please provide a role.")
    val role: String,
    @field:NotBlank(message = "Please provide an endpoint ID.")
    val endpointId: String,
)