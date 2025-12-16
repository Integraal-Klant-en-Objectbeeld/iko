package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.UniqueRelation
import com.ritense.iko.mvc.model.validation.UniqueRelationCheck
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.util.UUID
import com.ritense.iko.aggregateddataprofile.domain.Relation as RelationEntity

@UniqueRelationCheck
data class EditRelationForm(
    override val aggregatedDataProfileId: UUID,
    override val id: UUID,
    override val sourceId: UUID,
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToEndpointMapping: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val transform: String,
    @field:NotBlank(message = "Please provide a property name.")
    override val propertyName: String,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    val cacheEnabled: Boolean,
    @field:Positive(message = "Please provide a positive value.")
    val cacheTimeToLive: Int,
) : UniqueRelation {
    companion object {
        fun from(it: RelationEntity): EditRelationForm = EditRelationForm(
            aggregatedDataProfileId = it.aggregatedDataProfile.id,
            id = it.id,
            sourceId = it.sourceId ?: it.aggregatedDataProfile.id,
            sourceToEndpointMapping = it.sourceToEndpointMapping.expression,
            transform = it.transform.expression,
            connectorInstanceId = it.connectorInstanceId,
            connectorEndpointId = it.connectorEndpointId,
            propertyName = it.propertyName,
            cacheEnabled = it.relationCacheSettings.enabled,
            cacheTimeToLive = it.relationCacheSettings.timeToLive,
        )
    }
}