package com.ritense.iko.connectors.bag

import com.ritense.iko.connectors.bag.endpoints.BagEndpointAdresseerbareObjecten
import com.ritense.iko.connectors.bag.endpoints.BagEndpointAdressen
import com.ritense.iko.connectors.bag.endpoints.BagEndpointBronhouders
import com.ritense.iko.connectors.bag.endpoints.BagEndpointLigplaatsen
import com.ritense.iko.connectors.bag.endpoints.BagEndpointNummeraanduidingen
import com.ritense.iko.connectors.bag.endpoints.BagEndpointOpenbareRuimten
import com.ritense.iko.connectors.bag.endpoints.BagEndpointPanden
import com.ritense.iko.connectors.bag.endpoints.BagEndpointStandplaatsen
import com.ritense.iko.connectors.bag.endpoints.BagEndpointVerblijfsobjecten
import com.ritense.iko.connectors.bag.endpoints.BagEndpointWoonplaatsen
import com.ritense.iko.endpoints.PublicEndpoints

class BagPublicEndpoints : PublicEndpoints() {
    override fun configure() {

        handleAccessDeniedException()

        id("/bag/adresseerbareObjecten", BagEndpointAdresseerbareObjecten.URI)
        search("/bag/adresseerbareObjecten", BagEndpointAdresseerbareObjecten.URI)

        id("/bag/adressen", BagEndpointAdressen.URI)
        search("/bag/adressen", BagEndpointAdressen.URI)

        id("/bag/bronhouders", BagEndpointBronhouders.URI)
        search("/bag/bronhouders", BagEndpointBronhouders.URI)

        id("/bag/ligplaatsen", BagEndpointLigplaatsen.URI)
        search("/bag/ligplaatsen", BagEndpointLigplaatsen.URI)

        id("/bag/nummeraanduidingen", BagEndpointNummeraanduidingen.URI)
        search("/bag/nummeraanduidingen", BagEndpointNummeraanduidingen.URI)

        id("/bag/openbareRuimten", BagEndpointOpenbareRuimten.URI)
        search("/bag/openbareRuimten", BagEndpointOpenbareRuimten.URI)

        id("/bag/panden", BagEndpointPanden.URI)
        search("/bag/panden", BagEndpointPanden.URI)

        id("/bag/standplaatsen", BagEndpointStandplaatsen.URI)
        search("/bag/standplaatsen", BagEndpointStandplaatsen.URI)

        id("/bag/verblijfsobjecten", BagEndpointVerblijfsobjecten.URI)
        search("/bag/verblijfsobjecten", BagEndpointVerblijfsobjecten.URI)

        id("/bag/woonplaatsen", BagEndpointWoonplaatsen.URI)
        search("/bag/woonplaatsen", BagEndpointWoonplaatsen.URI)
    }

}
