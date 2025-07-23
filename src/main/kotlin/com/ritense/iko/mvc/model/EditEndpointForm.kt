package com.ritense.iko.mvc.model

import com.ritense.iko.endpoints.Endpoint
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class EditEndpointForm(
    val id: UUID,
    @field:NotBlank(message = "Please provide a name.")
    val name: String,
    val routeId: String? = null,
    val isPrimary: Boolean,
    val isActive: Boolean
) {
    companion object {
        fun from(endpoint: Endpoint): EditEndpointForm {
            return EditEndpointForm(
                id = endpoint.id,
                name = endpoint.name,
                routeId = endpoint.routeId,
                isPrimary = endpoint.isPrimary,
                isActive = endpoint.isActive
            )
        }
    }
}
