package com.ritense.iko.mvc.model

import java.util.UUID
import com.ritense.iko.aggregateddataprofile.domain.Relation as RelationEntity

data class DeleteRelationForm(
    val aggregatedDataProfileId: UUID,
    val id: UUID,
) {
    companion object {
        fun from(it: RelationEntity): DeleteRelationForm = DeleteRelationForm(
            aggregatedDataProfileId = it.aggregatedDataProfile.id,
            id = it.id,
        )
    }
}