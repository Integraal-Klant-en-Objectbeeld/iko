package com.ritense.iko.mvc.model

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.mvc.model.validation.UniqueName
import com.ritense.iko.mvc.model.validation.UniqueNameForm
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.UUID

@UniqueName
data class AggregatedDataProfileEditForm(
    val id: UUID,
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
    val role: String? = null,
    var connectorInstanceId: UUID,
    var connectorEndpointId: UUID,
    val cacheEnabled: Boolean,
    val cacheTimeToLive: Int
) : UniqueNameForm {

    companion object {
        fun from(aggregatedDataProfile: AggregatedDataProfile) = AggregatedDataProfileEditForm(
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