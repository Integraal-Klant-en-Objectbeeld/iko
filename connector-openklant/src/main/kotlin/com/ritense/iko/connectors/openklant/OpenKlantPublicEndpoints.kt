package com.ritense.iko.connectors.openklant

import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointActorKlantContacten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointBetrokkenen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointBijlagen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointCategorieRelaties
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointCategorieen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointDigitaleAdressen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointInternetaken
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointKlantContacten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointOnderwerpObjecten
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointPartijIdentificatoren
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointPartijen
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointRekeningnummers
import com.ritense.iko.connectors.openklant.endpoints.OpenKlantEndpointVertegenwoordigingen
import com.ritense.iko.endpoints.PublicEndpoints
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

class OpenKlantPublicEndpoints : PublicEndpoints() {
    override fun configure() {
        handleAccessDeniedException()

        val exposedOperations = listOf(
            "actoren",
            "betrokkenen"
        );

        custom()

        // /openklant/config/actoren?query=1
        // /openklant/config/actoren/{id}?query=1

        // /openklant/config/betrokkenen?query=1
        // /openklant/config/betrokkenen/{id}?query=1

//        idWithConfig("openklant", "actoren", OpenKlantEndpointActoren.URI)
        // Actoren endpoints
//        id("/openklant/actoren", OpenKlantEndpointActoren.URI)
//        endpointWithConfig("openklant", "actoren", OpenKlantEndpointActoren.URI)


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

    fun custom() {
//        rest("/endpoints")
//            .get("/{iko:module}/{iko:config}/{iko:operation}")
//            .to("direct:iko:rest:endpoint")
//            .get("/{iko:module}/{iko:config}/{iko:operation}/{id}")
//            .to("direct:iko:rest:endpoint.id")
//
//        from("direct:rest:endpoint")
//            .errorHandler(noErrorHandler())
//            .setVariable("module", header("iko:module"))
//            .setVariable("config", header("iko:config"))
//            .setVariable("operation", header("iko:operation"))
//            .removeHeaders("iko:*")
//            .to("direct:iko:validate.endpoint")
//            .to("direct:iko:auth")
//
//            .to("direct:iko:transform:\${variable.module}.\${variable.config}.\${variable.operation}")
//            .to("direct:iko:api:\${variable.module}")
//            .marshal().json()
//
//        from("direct:rest:endpoint.id")
//            .errorHandler(noErrorHandler())
//            .setVariable("module", header("iko:module"))
//            .setVariable("config", header("iko:config"))
//            .setVariable("operation", header("iko:operation"))
//            .setVariable("id", header("iko:id"))
//            .removeHeaders("iko:*")
//            .to("direct:iko:validate:endpoint")
//            .to("direct:iko:auth:2")
//            .to("direct:iko:transform:\${variable.module}.\${variable.config}.\${variable.operation}")
//            .to("direct:iko:api:\${variable.module}")
//            .marshal().json()
//
//        from("direct:validate:endpoint")
//            .errorHandler(noErrorHandler())
//            .process { ex ->
//                // Hey, heb ik een module die correct is heb ik een config dat correct is en de operatie geldig is?
//            }
//            .log("Validating endpoint: \${header.module}.\${header.config}.\${header.operation}.\${header.id}")
//
//        from("direct:endpoint:\${variable.module}.\${variable.eopration}")
//
//        from("direct:auth:2")
//            .errorHandler(noErrorHandler())
//            .log("Authorizing endpoint: \${header.module}.\${header.config}.\${header.operation}.\${header.id}")
//            .process { ex ->
//                val module = ex.getVariable("module", String::class.java)
//                val config = ex.getVariable("config", String::class.java)
//                val operation = ex.getVariable("operation", String::class.java)
//
//                // module, config operation -> zoek in de database wat de rol is die hier bij past.
//                listOf("ROLE_ENDPOINT_${module}_${config}_${operation}").let {
//                    log.debug("Authorizing endpoint with authority: $it")
//                    if (it.isEmpty()) {
//                        return@process
//                    }
//
//                    if (SecurityContextHolder.getContext().authentication != null && SecurityContextHolder.getContext().authentication.authorities.any { x ->
//                            it.contains(x.authority)
//                        }) {
//                        return@process
//                    }
//
//                    throw AccessDeniedException("User is not authorized to perform access this route. Missing authorities: $it")
//                }
//            }
    }
}

class OperationRepository() {
    fun findByModuleAndOperation(module: String, operation: String): Operation? {
        return Operation(module, operation, "ROLE_ENDPOINT_${module}_${operation}")
    }
}

class ConfigRepository() {
    fun findByModuleAndConfig(module: String, config: String): Config? {
        return Config(module, config, emptyMap())
    }
}

data class Operation(
    val module: String,
    val operation: String,
    val role: String
)

data class Config(
    val module: String,
    val config: String,
    val properties: Map<String, String>
)