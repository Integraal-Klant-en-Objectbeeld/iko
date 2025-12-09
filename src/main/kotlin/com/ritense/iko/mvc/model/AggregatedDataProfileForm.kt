package com.ritense.iko.mvc.model

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.mvc.model.validation.UniqueName
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.util.UUID

@UniqueName
data class AggregatedDataProfileForm(
    val id: UUID? = null,
    @field:NotBlank(message = "Please provide a name.")
    @field:Pattern(
        regexp = "[0-9a-zA-Z_\\-]+",
        message = "Name may only contain letters, digits, underscores, and hyphens.",
    )
    val name: String? = null,
    @field:ValidTransform
    @field:NotBlank(message = "Please provide a transform expression.")
    val transform: String? = null,
    @field:NotBlank(message = "Please provide a role.")
    val role: String? = null,
    @field:NotNull(message = "Please provide a connector instance.")
    var connectorInstanceId: UUID? = null,
    @field:NotNull(message = "Please provide a connector endpoint.")
    var connectorEndpointId: UUID? = null,
    val cacheEnabled: Boolean,
    val cacheTimeToLive: Int
) {

    companion object {
        fun from(aggregatedDataProfile: AggregatedDataProfile) = AggregatedDataProfileForm(
            id = aggregatedDataProfile.id,
            name = aggregatedDataProfile.name,
            role = aggregatedDataProfile.role,
            transform = aggregatedDataProfile.transform.expression,
            connectorInstanceId = aggregatedDataProfile.connectorInstanceId,
            connectorEndpointId = aggregatedDataProfile.connectorEndpointId,
            cacheEnabled = aggregatedDataProfile.aggregatedDataProfileCacheSetting.enabled,
            cacheTimeToLive = aggregatedDataProfile.aggregatedDataProfileCacheSetting.timeToLive,
        )
    }

}