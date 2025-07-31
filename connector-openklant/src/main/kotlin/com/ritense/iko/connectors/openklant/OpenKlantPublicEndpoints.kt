package com.ritense.iko.connectors.openklant

import com.ritense.iko.connectors.openklant.endpoints.*
import com.ritense.iko.endpoints.PublicEndpoints

class OpenKlantPublicEndpoints : PublicEndpoints() {
    override fun configure() {
        handleAccessDeniedException()

        // Actoren endpoints
        id("/openklant/actoren", OpenKlantEndpointActoren.URI)
        endpoint("/openklant/actoren", OpenKlantEndpointActoren.URI)

        // ActorKlantContacten endpoints
        id("/openklant/actorklantcontacten", OpenKlantEndpointActorKlantContacten.URI)
        endpoint("/openklant/actorklantcontacten", OpenKlantEndpointActorKlantContacten.URI)

        // Betrokkenen endpoints
        id("/openklant/betrokkenen", OpenKlantEndpointBetrokkenen.URI)
        endpoint("/openklant/betrokkenen", OpenKlantEndpointBetrokkenen.URI)

        // Bijlagen endpoints
        id("/openklant/bijlagen", OpenKlantEndpointBijlagen.URI)
        endpoint("/openklant/bijlagen", OpenKlantEndpointBijlagen.URI)

        // CategorieRelaties endpoints
        id("/openklant/categorie-relaties", OpenKlantEndpointCategorieRelaties.URI)
        endpoint("/openklant/categorie-relaties", OpenKlantEndpointCategorieRelaties.URI)

        // Categorieen endpoints
        id("/openklant/categorieen", OpenKlantEndpointCategorieen.URI)
        endpoint("/openklant/categorieen", OpenKlantEndpointCategorieen.URI)

        // DigitaleAdressen endpoints
        id("/openklant/digitaleadressen", OpenKlantEndpointDigitaleAdressen.URI)
        endpoint("/openklant/digitaleadressen", OpenKlantEndpointDigitaleAdressen.URI)

        // Internetaken endpoints
        id("/openklant/internetaken", OpenKlantEndpointInternetaken.URI)
        endpoint("/openklant/internetaken", OpenKlantEndpointInternetaken.URI)

        // KlantContacten endpoints
        id("/openklant/klantcontacten", OpenKlantEndpointKlantContacten.URI)
        endpoint("/openklant/klantcontacten", OpenKlantEndpointKlantContacten.URI)

        // OnderwerpObjecten endpoints
        id("/openklant/onderwerpobjecten", OpenKlantEndpointOnderwerpObjecten.URI)
        endpoint("/openklant/onderwerpobjecten", OpenKlantEndpointOnderwerpObjecten.URI)

        // PartijIdentificatoren endpoints
        id("/openklant/partij-identificatoren", OpenKlantEndpointPartijIdentificatoren.URI)
        endpoint("/openklant/partij-identificatoren", OpenKlantEndpointPartijIdentificatoren.URI)

        // Partijen endpoints
        id("/openklant/partijen", OpenKlantEndpointPartijen.URI)
        endpoint("/openklant/partijen", OpenKlantEndpointPartijen.URI)

        // Rekeningnummers endpoints
        id("/openklant/rekeningnummers", OpenKlantEndpointRekeningnummers.URI)
        endpoint("/openklant/rekeningnummers", OpenKlantEndpointRekeningnummers.URI)

        // Vertegenwoordigingen endpoints
        id("/openklant/vertegenwoordigingen", OpenKlantEndpointVertegenwoordigingen.URI)
        endpoint("/openklant/vertegenwoordigingen", OpenKlantEndpointVertegenwoordigingen.URI)
    }
}