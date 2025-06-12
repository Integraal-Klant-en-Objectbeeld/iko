package com.ritense.iko.mvc.model

import com.ritense.iko.search.Search
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class EditSearchForm(
    val id: UUID,
    @field:NotBlank(message = "Please provide a name.")
    val name: String,
    @field:NotBlank(message = "Please select a Operation.")
    val routeId: String,
    @field:NotBlank(message = "Please choose a primary.")
    val isPrimary: Boolean
) {
    companion object {
        fun from(search: Search): EditSearchForm {
            return EditSearchForm(
                id = search.id,
                name = search.name,
                routeId = search.routeId,
                isPrimary = search.isPrimary
            )
        }
    }
}
