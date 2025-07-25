package com.ritense.iko.mvc.model

import com.ritense.iko.aggregateddataprofile.AggregatedDataProfile
import com.ritense.iko.mvc.model.validation.UniqueName
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class EditAggregatedDataProfileForm(
    val id: UUID,
    @field:NotBlank(message = "Please provide a name.")
    @field:UniqueName
    val name: String,
    @field:NotBlank(message = "Please select a primary endpoint.")
    val primaryEndpoint: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val transform: String
) {

    companion object {
        fun from(aggregatedDataProfile: AggregatedDataProfile): EditAggregatedDataProfileForm {
            return EditAggregatedDataProfileForm(
                id = aggregatedDataProfile.id,
                name = aggregatedDataProfile.name,
                primaryEndpoint = aggregatedDataProfile.primaryEndpoint.toString(),
                transform = aggregatedDataProfile.transform.expression
            )
        }
    }
}
