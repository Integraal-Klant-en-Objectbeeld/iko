package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AddRelationForm(
    val aggregatedDataProfileId: UUID,
    val sourceId: String?,
    @field:NotNull(message = "Please provide a connector instance.")
    var connectorInstanceId: UUID? = null,
    @field:NotNull(message = "Please provide a connector endpoint.")
    var connectorEndpointId: UUID? = null,
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToEndpointMapping: String? = null,
    @field:NotBlank(message = "Please define a transform expression.")
    @field:ValidTransform
    var resultTransform: String? = null,
    @field:NotBlank(message = "Please provide a property name.")
    var propertyName: String? = null,
)