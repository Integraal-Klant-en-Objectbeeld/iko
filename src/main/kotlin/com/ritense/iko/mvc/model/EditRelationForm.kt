package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import com.ritense.iko.aggregateddataprofile.Relation as RelationEntity

data class EditRelationForm(
    val aggregatedDataProfileId: UUID,
    val id: UUID,
    val sourceId: String? = "",
    @field:NotBlank(message = "Please select a endpoint.")
    val endpointId: String,
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToEndpointMapping: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val transform: String
) {
    companion object {
        fun from(it: RelationEntity): EditRelationForm {
            return EditRelationForm(
                aggregatedDataProfileId = it.aggregatedDataProfile.id,
                id = it.id,
                sourceId = it.sourceId?.toString(),
                endpointId = it.endpointId,
                sourceToEndpointMapping = it.sourceToEndpointMapping,
                transform = it.transform.expression
            )
        }
    }
}