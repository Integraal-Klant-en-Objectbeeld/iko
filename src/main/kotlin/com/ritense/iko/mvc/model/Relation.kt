package com.ritense.iko.mvc.model

import java.util.UUID
import com.ritense.iko.profile.Relation as RelationEntity

data class Relation(
    val profileId: UUID,
    val id: UUID,
    val sourceId: String,
    val endpointId: String,
    val sourceToSearchMapping: String,
    val transform: String
) {
    companion object {
        fun from(it: RelationEntity): Relation {
            return Relation(
                profileId = it.profile.id,
                id = it.id,
                sourceId = it.sourceId?.toString() ?: "Profile root",
                endpointId = it.endpointId,
                sourceToSearchMapping = it.sourceToSearchMapping,
                transform = it.transform.expression
            )
        }
    }
}