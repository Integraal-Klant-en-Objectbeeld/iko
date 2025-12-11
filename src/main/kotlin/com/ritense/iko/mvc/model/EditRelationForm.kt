package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import com.ritense.iko.aggregateddataprofile.domain.Relation as RelationEntity

data class EditRelationForm(
    val aggregatedDataProfileId: UUID,
    val id: UUID,
    val sourceId: String? = "",
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToEndpointMapping: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val resultTransform: String,
    @field:NotBlank(message = "Please provide a property name.")
    val propertyName: String,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
) {
    companion object {
        fun from(it: RelationEntity): EditRelationForm = EditRelationForm(
            aggregatedDataProfileId = it.aggregatedDataProfile.id,
            id = it.id,
            sourceId = it.sourceId?.toString(),
            sourceToEndpointMapping = it.sourceToEndpointMapping,
            resultTransform = it.resultTransform.expression,
            connectorInstanceId = it.connectorInstanceId,
            connectorEndpointId = it.connectorEndpointId,
            propertyName = it.propertyName,
        )
    }
}