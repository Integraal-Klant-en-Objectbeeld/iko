package com.ritense.iko.connectors.openzaak

import com.ritense.iko.endpoints.PublicEndpoints
import com.ritense.iko.connectors.brp.ValidationException
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointResultaten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointRollen
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointStatussen
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakContactMomenten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakInformatieObjecten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakObjecten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakVerzoeken
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaken
import com.ritense.iko.search.PublicSearchEndpoints
import org.apache.camel.Exchange

class OpenZaakPublicEndpoints : PublicSearchEndpoints() {
    override fun configure() {
        handleAccessDeniedException()

        id("/openZaak/zaken", OpenZaakEndpointZaken.URI)
        search("/openZaak/zaken", OpenZaakEndpointZaken.URI)

        id("/openZaak/zaakVerzoeken", OpenZaakEndpointZaakVerzoeken.URI)
        search("/openZaak/zaakVerzoeken", OpenZaakEndpointZaakVerzoeken.URI)

        id("/openZaak/zaakObjecten", OpenZaakEndpointZaakObjecten.URI)
        search("/openZaak/zaakObjecten", OpenZaakEndpointZaakObjecten.URI)

        id("/openZaak/zaakInformatieObjecten", OpenZaakEndpointZaakInformatieObjecten.URI)
        search("/openZaak/zaakInformatieObjecten", OpenZaakEndpointZaakInformatieObjecten.URI)

        id("/openZaak/zaakContactMomenten", OpenZaakEndpointZaakContactMomenten.URI)
        search("/openZaak/zaakContactMomenten", OpenZaakEndpointZaakContactMomenten.URI)

        id("/openZaak/statussen", OpenZaakEndpointStatussen.URI)
        search("/openZaak/statussen", OpenZaakEndpointStatussen.URI)

        id("/openZaak/rollen", OpenZaakEndpointRollen.URI)
        search("/openZaak/rollen", OpenZaakEndpointRollen.URI)

        id("/openZaak/resultaten", OpenZaakEndpointResultaten.URI)
        search("/openZaak/resultaten", OpenZaakEndpointResultaten.URI)
    }
}