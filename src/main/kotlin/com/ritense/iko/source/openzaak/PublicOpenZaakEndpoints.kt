package com.ritense.iko.source.openzaak

import com.ritense.iko.search.PublicSearchEndpoints
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchResultaten
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchRollen
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchStatussen
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaakContactMomenten
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaakInformatieObjecten
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaakObjecten
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaakVerzoeken
import com.ritense.iko.source.openzaak.searches.OpenZaakSearchZaken

class PublicOpenZaakEndpoints : PublicSearchEndpoints() {
    override fun configure() {

        handleAccessDeniedException()

        id("/openZaak/zaken", OpenZaakSearchZaken.URI)
        search("/openZaak/zaken", OpenZaakSearchZaken.URI)

        id("/openZaak/zaakVerzoeken", OpenZaakSearchZaakVerzoeken.URI, )
        search("/openZaak/zaakVerzoeken", OpenZaakSearchZaakVerzoeken.URI)

        id("/openZaak/zaakObjecten", OpenZaakSearchZaakObjecten.URI)
        search("/openZaak/zaakObjecten", OpenZaakSearchZaakObjecten.URI)

        id("/openZaak/zaakInformatieObjecten", OpenZaakSearchZaakInformatieObjecten.URI)
        search("/openZaak/zaakInformatieObjecten", OpenZaakSearchZaakInformatieObjecten.URI)

        id("/openZaak/zaakContactMomenten", OpenZaakSearchZaakContactMomenten.URI)
        search("/openZaak/zaakContactMomenten", OpenZaakSearchZaakContactMomenten.URI)

        id("/openZaak/statussen", OpenZaakSearchStatussen.URI)
        search("/openZaak/statussen", OpenZaakSearchStatussen.URI)

        id("/openZaak/rollen", OpenZaakSearchRollen.URI)
        search("/openZaak/rollen", OpenZaakSearchRollen.URI)

        id("/openZaak/resultaten", OpenZaakSearchResultaten.URI)
        search("/openZaak/resultaten", OpenZaakSearchResultaten.URI)
    }
}