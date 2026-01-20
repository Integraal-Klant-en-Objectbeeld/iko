package com.ritense.iko.mvc.model

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.mvc.model.validation.UniqueAggregatedDataProfile
import com.ritense.iko.mvc.model.validation.UniqueAggregatedDataProfileCheck
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.UUID

@UniqueAggregatedDataProfileCheck
data class AggregatedDataProfileEditForm(
    override val id: UUID,
    @field:NotBlank(message = "Please provide a name.")
    @field:Pattern(
        regexp = "[0-9a-zA-Z_\\-]+",
        message = "Name may only contain letters, digits, underscores, and hyphens.",
    )
    override val name: String,
    @field:NotBlank(message = "Please provide roles.")
    @field:Pattern(
        regexp = "^ROLE_[A-Z0-9_]+(,ROLE_[A-Z0-9_]+)*$",
        message = "Roles must be a comma-separated list of ROLE_xxx values (e.g., ROLE_ADMIN,ROLE_USER).",
    )
    val roles: String,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    @field:ValidTransform
    @field:NotBlank(message = "Please provide a transform expression.")
    val endpointTransform: String,
    @field:ValidTransform
    @field:NotBlank(message = "Please provide a transform expression.")
    val resultTransform: String,
    val cacheEnabled: Boolean,
    @field:Min(value = 0)
    val cacheTimeToLive: Int,
) : UniqueAggregatedDataProfile {

    companion object {
        fun from(aggregatedDataProfile: AggregatedDataProfile) = AggregatedDataProfileEditForm(
            id = aggregatedDataProfile.id,
            name = aggregatedDataProfile.name,
            roles = aggregatedDataProfile.roles.value,
            connectorInstanceId = aggregatedDataProfile.connectorInstanceId,
            connectorEndpointId = aggregatedDataProfile.connectorEndpointId,
            endpointTransform = aggregatedDataProfile.endpointTransform.expression,
            resultTransform = aggregatedDataProfile.resultTransform.expression,
            cacheEnabled = aggregatedDataProfile.aggregatedDataProfileCacheSetting.enabled,
            cacheTimeToLive = aggregatedDataProfile.aggregatedDataProfileCacheSetting.timeToLive,
        )
    }
}