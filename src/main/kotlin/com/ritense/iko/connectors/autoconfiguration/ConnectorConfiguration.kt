package com.ritense.iko.connectors.autoconfiguration

import com.ritense.authzenk.Client
import com.ritense.iko.connectors.camel.Connector
import com.ritense.iko.connectors.camel.ConnectorConfig
import com.ritense.iko.connectors.camel.Endpoint
import com.ritense.iko.connectors.camel.EndpointAuth
import com.ritense.iko.connectors.camel.EndpointValidation
import com.ritense.iko.connectors.camel.Transform
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorEndpointRoleRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import com.ritense.iko.connectors.repository.ConnectorRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.CamelContext
import org.apache.camel.support.PluginHelper
import org.apache.camel.support.ResourceHelper
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener

@Configuration
class ConnectorConfiguration(val connectorRepository: ConnectorRepository) {

    @Bean
    fun endpoint() = Endpoint()

    @Bean
    fun endpointAuth(
        connectorEndpointRepository: ConnectorEndpointRepository,
        connectorInstanceRepository: ConnectorInstanceRepository,
        connectorEndpointRoleRepository: ConnectorEndpointRoleRepository,
        pdpClient: Client
    ) = EndpointAuth(
        connectorEndpointRepository,
        connectorInstanceRepository,
        connectorEndpointRoleRepository,
        pdpClient
    )

    @Bean
    fun endpointValidation(
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
    fun loadAllConnectorsAtStartup(event: ApplicationReadyEvent) {
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