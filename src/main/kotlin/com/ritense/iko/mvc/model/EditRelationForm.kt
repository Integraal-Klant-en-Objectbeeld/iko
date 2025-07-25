package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import com.ritense.iko.profile.Relation as RelationEntity

data class EditRelationForm(
    val profileId: UUID,
    val id: UUID,
    val sourceId: String? = "",
    @field:NotBlank(message = "Please select a endpoint.")
    val endpointId: String,
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToSearchMapping: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val transform: String
) {
    companion object {
        fun from(it: RelationEntity): EditRelationForm {
            return EditRelationForm(
                profileId = it.profile.id,
                id = it.id,
                sourceId = it.sourceId?.toString(),
                endpointId = it.endpointId,
                sourceToSearchMapping = it.sourceToSearchMapping,
                transform = it.transform.expression
            )
        }
    }
}