package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.UniqueName
import com.ritense.iko.mvc.model.validation.ValidTransform
import com.ritense.iko.profile.Profile
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class EditProfileForm(
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
        fun from(profile: Profile): EditProfileForm {
            return EditProfileForm(
                id = profile.id,
                name = profile.name,
                primaryEndpoint = profile.primaryEndpoint.toString(),
                transform = profile.transform.expression
            )
        }
    }
}
