package com.ritense.iko.mvc.model

import com.ritense.iko.profile.Profile
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class EditProfileForm(
    val id: UUID,
    @field:NotBlank(message = "Please provide a name.")
    val name: String,
    @field:NotBlank(message = "Please select a primary source.")
    val primarySource: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val transform: String
) {

    companion object {
        fun from(profile: Profile): EditProfileForm {
            return EditProfileForm(
                id = profile.id,
                name = profile.name,
                primarySource = profile.primarySource,
                transform = profile.transform.expression
            )
        }
    }
}
