package com.ritense.iko.zaken

import com.ritense.iko.brp.ValidationException
import com.ritense.iko.search.PublicSearchEndpoints
import com.ritense.iko.zaken.searches.OpenZaakSearch
import com.ritense.iko.zaken.searches.OpenZaakSearchResultaten
import com.ritense.iko.zaken.searches.OpenZaakSearchRollen
import com.ritense.iko.zaken.searches.OpenZaakSearchStatussen
import com.ritense.iko.zaken.searches.OpenZaakSearchZaakContactMomenten
import com.ritense.iko.zaken.searches.OpenZaakSearchZaakInformatieObjecten
import com.ritense.iko.zaken.searches.OpenZaakSearchZaakObjecten
import com.ritense.iko.zaken.searches.OpenZaakSearchZaakVerzoeken
import com.ritense.iko.zaken.searches.OpenZaakSearchZaken
import org.apache.camel.Exchange
import org.springframework.stereotype.Component

@Component
class PublicOpenZaakEndpoints : PublicSearchEndpoints() {
    override fun configure() {

        onException(ValidationException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("[]"))

        id("/openZaak/zaken", OpenZaakSearchZaken.URI)
        search("/openZaak/zaken", OpenZaakSearchZaken.URI)

        id("/openZaak/zaakVerzoeken", OpenZaakSearchZaakVerzoeken.URI)
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