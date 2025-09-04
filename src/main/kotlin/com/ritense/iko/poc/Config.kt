package com.ritense.iko.poc

import com.ritense.iko.poc.db.ConnectorEndpointRepository
import com.ritense.iko.poc.db.ConnectorEndpointRoleRepository
import com.ritense.iko.poc.db.ConnectorInstanceRepository
import com.ritense.iko.poc.db.ConnectorRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.CamelContext
import org.apache.camel.support.PluginHelper
import org.apache.camel.support.ResourceHelper
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener


@Configuration
class Config(val connectorRepository: ConnectorRepository) {

    @Bean
    fun pocEndpoint() = Endpoint()

    @Bean
    fun pocEndpointAuth(
        connectorEndpointRepository: ConnectorEndpointRepository,
        connectorInstanceRepository: ConnectorInstanceRepository,
        connectorEndpointRoleRepository: ConnectorEndpointRoleRepository
    ) = EndpointAuth(
        connectorEndpointRepository,
        connectorInstanceRepository,
        connectorEndpointRoleRepository
    )

    @Bean
    fun pocEndpointValidation(
        connectorEndpointRepository: ConnectorEndpointRepository,
        connectorInstanceRepository: ConnectorInstanceRepository
    ) = EndpointValidation(connectorEndpointRepository, connectorInstanceRepository)

    @Bean
    fun ikoConnector() = Connector()

    @Bean
    fun ikoConnectorConfig(connectorInstanceRepository: ConnectorInstanceRepository) =
        ConnectorConfig(connectorInstanceRepository)

    @Bean
    fun ikoTransform() = Transform()

    @EventListener(
        ApplicationReadyEvent::class
    )
    fun openklant(event: ApplicationReadyEvent) {
        val camelContext = event.applicationContext.getBean("camelContext") as CamelContext

        connectorRepository.findAll()
            .forEach {
                try {
                    val resource = ResourceHelper.fromBytes(
                        "${it.tag}.yaml", it.connectorCode.toByteArray()
                    )
                    PluginHelper.getRoutesLoader(camelContext).loadRoutes(resource)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load connector ${it.tag}" }
                }
            }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}