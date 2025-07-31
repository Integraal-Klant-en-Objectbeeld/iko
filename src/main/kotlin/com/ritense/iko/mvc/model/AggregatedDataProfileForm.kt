package com.ritense.iko.mvc.model

import com.ritense.iko.aggregateddataprofile.AggregatedDataProfile
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
    @field:NotBlank(message = "Please select a primary endpoint.")
    val primaryEndpoint: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val transform: String,
    val role: String? = null,
) {

    companion object {
        fun from(aggregatedDataProfile: AggregatedDataProfile): AggregatedDataProfileForm {
            return AggregatedDataProfileForm(
                id = aggregatedDataProfile.id,
                name = aggregatedDataProfile.name,
                role = aggregatedDataProfile.role,
                primaryEndpoint = aggregatedDataProfile.primaryEndpoint.toString(),
                transform = aggregatedDataProfile.transform.expression
            )
        }
    }
}
