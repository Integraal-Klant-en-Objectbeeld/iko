package com.ritense.iko.connectors.objectenapi

import com.ritense.iko.connectors.objectenapi.endpoints.ObjectenApiEndpointObjects
import com.ritense.iko.endpoints.PublicEndpoints

class ObjectenApiPublicEndpoints : PublicEndpoints() {
    override fun configure() {
        handleAccessDeniedException()

        id("/objecten", ObjectenApiEndpointObjects.URI)
        search("/objecten", ObjectenApiEndpointObjects.URI)
    }
}