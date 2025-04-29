package com.ritense.iko.source.bag

import com.ritense.iko.source.bag.search.BagSearchAdresseerbareObjecten
import com.ritense.iko.source.bag.search.BagSearchAdressen
import com.ritense.iko.source.bag.search.BagSearchBronhouders
import com.ritense.iko.source.bag.search.BagSearchLigplaatsen
import com.ritense.iko.source.bag.search.BagSearchNummeraanduidingen
import com.ritense.iko.source.bag.search.BagSearchOpenbareRuimten
import com.ritense.iko.source.bag.search.BagSearchPanden
import com.ritense.iko.source.bag.search.BagSearchStandplaatsen
import com.ritense.iko.source.bag.search.BagSearchVerblijfsobjecten
import com.ritense.iko.source.bag.search.BagSearchWoonplaatsen
import com.ritense.iko.search.PublicSearchEndpoints
import com.ritense.iko.source.brp.ValidationException
import org.apache.camel.Exchange

class BagPublicEndpoints : PublicSearchEndpoints() {
    override fun configure() {
        onException(ValidationException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("[]"))

        id("/bag/adresseerbareObjecten", BagSearchAdresseerbareObjecten.URI)
        search("/bag/adresseerbareObjecten", BagSearchAdresseerbareObjecten.URI)

        id("/bag/adressen", BagSearchAdressen.URI)
        search("/bag/adressen", BagSearchAdressen.URI)

        id("/bag/bronhouders", BagSearchBronhouders.URI)
        search("/bag/bronhouders", BagSearchBronhouders.URI)

        id("/bag/ligplaatsen", BagSearchLigplaatsen.URI)
        search("/bag/ligplaatsen", BagSearchLigplaatsen.URI)

        id("/bag/nummeraanduidingen", BagSearchNummeraanduidingen.URI)
        search("/bag/nummeraanduidingen", BagSearchNummeraanduidingen.URI)

        id("/bag/openbareRuimten", BagSearchOpenbareRuimten.URI)
        search("/bag/openbareRuimten", BagSearchOpenbareRuimten.URI)

        id("/bag/panden", BagSearchPanden.URI)
        search("/bag/panden", BagSearchPanden.URI)

        id("/bag/standplaatsen", BagSearchStandplaatsen.URI)
        search("/bag/standplaatsen", BagSearchStandplaatsen.URI)

        id("/bag/verblijfsobjecten", BagSearchVerblijfsobjecten.URI)
        search("/bag/verblijfsobjecten", BagSearchVerblijfsobjecten.URI)

        id("/bag/woonplaatsen", BagSearchWoonplaatsen.URI)
        search("/bag/woonplaatsen", BagSearchWoonplaatsen.URI)
    }

}
