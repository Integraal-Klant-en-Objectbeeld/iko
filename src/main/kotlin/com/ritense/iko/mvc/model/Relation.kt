package com.ritense.iko.mvc.model

import java.util.UUID
import com.ritense.iko.aggregateddataprofile.Relation as RelationEntity

data class Relation(
    val aggregatedDataProfileId: UUID,
    val id: UUID,
    val sourceId: String,
    val endpointId: String,
    val sourceToEndpointMapping: String,
    val transform: String
) {
    companion object {
        fun from(it: RelationEntity): Relation {
            return Relation(
                aggregatedDataProfileId = it.aggregatedDataProfile.id,
                id = it.id,
                sourceId = it.sourceId?.toString() ?: "Profile root",
                endpointId = it.endpointId,
                sourceToEndpointMapping = it.sourceToEndpointMapping,
                transform = it.transform.expression
            )
        }
    }
}