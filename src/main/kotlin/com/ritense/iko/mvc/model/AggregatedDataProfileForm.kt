package com.ritense.iko.mvc.model

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.mvc.model.validation.UniqueName
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.UUID

@UniqueName
data class AggregatedDataProfileForm(
    val id: UUID? = null,
    @field:NotBlank(message = "Please provide a name.")
    @field:Pattern(
        regexp = "[0-9a-zA-Z_\\-]+",
        message = "Name may only contain letters, digits, underscores, and hyphens."
    )
    val name: String,
    @field:ValidTransform
    val endpointTransform: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val transform: String,
    val role: String? = null,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
) {

    companion object {
        fun from(aggregatedDataProfile: AggregatedDataProfile): AggregatedDataProfileForm {
            return AggregatedDataProfileForm(
                id = aggregatedDataProfile.id,
                name = aggregatedDataProfile.name,
                role = aggregatedDataProfile.role,
                endpointTransform = aggregatedDataProfile.endpointTransform.expression,
                transform = aggregatedDataProfile.transform.expression,
                connectorInstanceId = aggregatedDataProfile.connectorInstanceId,
                connectorEndpointId = aggregatedDataProfile.connectorEndpointId,
            )
        }
    }
}
