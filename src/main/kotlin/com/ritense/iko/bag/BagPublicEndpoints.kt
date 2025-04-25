package com.ritense.iko.bag

import com.ritense.iko.bag.search.BagSearchAdresseerbareObjecten
import com.ritense.iko.bag.search.BagSearchAdressen
import com.ritense.iko.bag.search.BagSearchBronhouders
import com.ritense.iko.bag.search.BagSearchLigplaatsen
import com.ritense.iko.bag.search.BagSearchNummeraanduidingen
import com.ritense.iko.bag.search.BagSearchOpenbareRuimten
import com.ritense.iko.bag.search.BagSearchPanden
import com.ritense.iko.bag.search.BagSearchStandplaatsen
import com.ritense.iko.bag.search.BagSearchVerblijfsobjecten
import com.ritense.iko.bag.search.BagSearchWoonplaatsen
import com.ritense.iko.brp.ValidationException
import com.ritense.iko.search.PublicSearchEndpoints
import org.apache.camel.Exchange
import org.springframework.stereotype.Component

@Component
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
