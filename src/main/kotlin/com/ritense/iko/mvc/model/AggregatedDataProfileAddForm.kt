package com.ritense.iko.mvc.model

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.mvc.model.validation.UniqueAggregatedDataProfile
import com.ritense.iko.mvc.model.validation.UniqueCollectionCheck
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.UUID

@UniqueCollectionCheck
data class AggregatedDataProfileAddForm(
    override val id: UUID? = null,
    @field:NotBlank(message = "Please provide a name.")
    @field:Pattern(
        regexp = "[0-9a-zA-Z_\\-]+",
        message = "Name may only contain letters, digits, underscores, and hyphens.",
    )
    override val name: String,
    @field:ValidTransform
    @field:NotBlank(message = "Please provide a transform expression.")
    val transform: String,
    @field:NotBlank(message = "Please provide a role.")
    val role: String,
    var connectorInstanceId: UUID,
    var connectorEndpointId: UUID
) : UniqueAggregatedDataProfile {

    companion object {
        fun from(aggregatedDataProfile: AggregatedDataProfile) = AggregatedDataProfileAddForm(
            name = aggregatedDataProfile.name,
            role = aggregatedDataProfile.role!!,
            transform = aggregatedDataProfile.transform.expression,
            connectorInstanceId = aggregatedDataProfile.connectorInstanceId,
            connectorEndpointId = aggregatedDataProfile.connectorEndpointId
        )
    }

}