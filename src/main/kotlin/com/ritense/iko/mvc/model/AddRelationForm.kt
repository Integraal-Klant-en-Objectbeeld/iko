package com.ritense.iko.mvc.model

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class AddRelationForm(
    val aggregatedDataProfileId: UUID,
    val sourceId: String?,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToEndpointMapping: String,
    @field:NotBlank(message = "Please define a transform expression.")
    var transform: String,
)
