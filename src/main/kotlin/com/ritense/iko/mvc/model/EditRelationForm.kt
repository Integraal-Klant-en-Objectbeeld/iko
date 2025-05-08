package com.ritense.iko.mvc.model

import jakarta.validation.constraints.NotBlank
import java.util.UUID
import com.ritense.iko.profile.Relation as RelationEntity

data class EditRelationForm(
    val profileId: UUID,
    val id: UUID,
    val sourceId: String? = "",
    @field:NotBlank(message = "Please select a search.")
    val searchId: String,
    @field:NotBlank(message = "sourceToSearchMapping cannot be blank")
    val sourceToSearchMapping: String,
    @field:NotBlank(message = "Transform cannot be blank")
    var transform: String,
) {
    companion object {
        fun from(it: RelationEntity): EditRelationForm {
            return EditRelationForm(
                profileId = it.profile.id,
                id = it.id,
                sourceId = it.sourceId.toString(),
                searchId = it.searchId,
                sourceToSearchMapping = it.sourceToSearchMapping,
                transform = it.transform.expression
            )
        }
    }
}