package com.ritense.iko.connectors.openklant

import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlanten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlantcontacten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointContactmomenten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointRollen
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
class OpenKlantPublicEndpoints : RouteBuilder() {
    override fun configure() {
        handleAccessDeniedException()

        id("/openklant/klanten", OpenKlantEndpointKlanten.URI)
        endpoint("/openklant/klanten", OpenKlantEndpointKlanten.URI)

        id("/openklant/klantcontacten", OpenKlantEndpointKlantcontacten.URI)
        endpoint("/openklant/klantcontacten", OpenKlantEndpointKlantcontacten.URI)

        id("/openklant/contactmomenten", OpenKlantEndpointContactmomenten.URI)
        endpoint("/openklant/contactmomenten", OpenKlantEndpointContactmomenten.URI)

        id("/openklant/rollen", OpenKlantEndpointRollen.URI)
        endpoint("/openklant/rollen", OpenKlantEndpointRollen.URI)
    }

    private fun id(uri: String, to: String) {
        rest("/endpoints$uri")
            .get("/{id}")
            .to("direct:${to}_id")

        from("direct:${to}_id")
            .errorHandler(noErrorHandler())
            .setVariable("authorities", constant("ROLE_ENDPOINT_${to.replace("direct:", "").uppercase()}"))
            .to("direct:auth")
            .routeId("direct:${to}_api_id")
            .to(to)
            .marshal().json()
    }

    private fun endpoint(uri: String, to: String) {
        rest("/endpoints$uri")
            .get()
            .to("direct:${to}_endpoint")

        from("direct:${to}_endpoint")
            .errorHandler(noErrorHandler())
            .setVariable("authorities", constant("ROLE_ENDPOINT_${to.replace("direct:", "").uppercase()}"))
            .to("direct:auth")
            .routeId("direct:${to}_api_endpoint")
            .to(to)
            .marshal().json()
    }

    private fun handleAccessDeniedException() {
        onException(AccessDeniedException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.UNAUTHORIZED.value()))
    }
}
