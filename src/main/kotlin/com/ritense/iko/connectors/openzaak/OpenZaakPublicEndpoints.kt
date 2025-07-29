package com.ritense.iko.connectors.openzaak

import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointResultaten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointRollen
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointStatussen
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakContactMomenten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakInformatieObjecten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakObjecten
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaakVerzoeken
import com.ritense.iko.connectors.openzaak.endpoints.OpenZaakEndpointZaken
import com.ritense.iko.endpoints.PublicEndpoints

class OpenZaakPublicEndpoints : PublicEndpoints() {
    override fun configure() {
        handleAccessDeniedException()

        id("/openZaak/zaken", OpenZaakEndpointZaken.URI)
        endpoint("/openZaak/zaken", OpenZaakEndpointZaken.URI)

        id("/openZaak/zaakVerzoeken", OpenZaakEndpointZaakVerzoeken.URI)
        endpoint("/openZaak/zaakVerzoeken", OpenZaakEndpointZaakVerzoeken.URI)

        id("/openZaak/zaakObjecten", OpenZaakEndpointZaakObjecten.URI)
        endpoint("/openZaak/zaakObjecten", OpenZaakEndpointZaakObjecten.URI)

        id("/openZaak/zaakInformatieObjecten", OpenZaakEndpointZaakInformatieObjecten.URI)
        endpoint("/openZaak/zaakInformatieObjecten", OpenZaakEndpointZaakInformatieObjecten.URI)

        id("/openZaak/zaakContactMomenten", OpenZaakEndpointZaakContactMomenten.URI)
        endpoint("/openZaak/zaakContactMomenten", OpenZaakEndpointZaakContactMomenten.URI)

        id("/openZaak/statussen", OpenZaakEndpointStatussen.URI)
        endpoint("/openZaak/statussen", OpenZaakEndpointStatussen.URI)

        id("/openZaak/rollen", OpenZaakEndpointRollen.URI)
        endpoint("/openZaak/rollen", OpenZaakEndpointRollen.URI)

        id("/openZaak/resultaten", OpenZaakEndpointResultaten.URI)
        endpoint("/openZaak/resultaten", OpenZaakEndpointResultaten.URI)
    }
}