package com.ritense.iko.mvc.model

import java.util.UUID
import com.ritense.iko.aggregateddataprofile.domain.Relation as RelationEntity

data class Relation(
    val aggregatedDataProfileId: UUID,
    val id: UUID,
    val sourceId: UUID,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    val propertyName: String,
    val sourceToEndpointMapping: String,
    val resultTransform: String,
) {
    companion object {
        fun from(it: RelationEntity): Relation = Relation(
            aggregatedDataProfileId = it.aggregatedDataProfile.id,
            id = it.id,
            sourceId = it.sourceId ?: it.aggregatedDataProfile.id,
            sourceToEndpointMapping = it.endpointTransform.expression,
            resultTransform = it.resultTransform.expression,
            connectorInstanceId = it.connectorInstanceId,
            connectorEndpointId = it.connectorEndpointId,
            propertyName = it.propertyName,
        )
    }
}