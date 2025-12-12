package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.UniqueRelation
import com.ritense.iko.mvc.model.validation.UniqueRelationCheck
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@UniqueRelationCheck
data class AddRelationForm(
    override val aggregatedDataProfileId: UUID,
    override val sourceId: UUID,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToEndpointMapping: String,
    @field:NotBlank(message = "Please define a transform expression.")
    @field:ValidTransform
    val transform: String,
    @field:NotBlank(message = "Please provide a property name.")
    override val propertyName: String,
    override val id: UUID? = null,
) : UniqueRelation