package com.ritense.iko.mvc.connector

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

class ConnectorInstanceRolesEditForm(
    @field:NotBlank(message = "Please provide a role.")
    val role: String,
    @field:NotBlank(message = "Please provide an endpoint ID.")
    val endpointId: String
)
