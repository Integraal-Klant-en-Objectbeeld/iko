/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.connectors.autoconfiguration

import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.connectors.camel.ConnectorConfigRouteBuilder
import com.ritense.iko.connectors.camel.ConnectorDispatcherRouteBuilder
import com.ritense.iko.connectors.camel.EndpointAuthRouteBuilder
import com.ritense.iko.connectors.camel.EndpointRestRoutesBuilder
import com.ritense.iko.connectors.camel.EndpointValidationRouteBuilder
import com.ritense.iko.connectors.camel.TransformDispatcheRouteBuilder
import com.ritense.iko.connectors.processor.ConnectorLookupProcessor
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorEndpointRoleRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import com.ritense.iko.connectors.repository.ConnectorRepository
import com.ritense.iko.connectors.service.ConnectorService
import com.ritense.iko.connectors.service.RouteDependencyService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.CamelContext
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order

@Configuration
class ConnectorConfiguration(
    val connectorRepository: ConnectorRepository,
) {
    @Bean
    fun connectorService(
        connectorInstanceRepository: ConnectorInstanceRepository,
        connectorEndpointRepository: ConnectorEndpointRepository,
        connectorEndpointRoleRepository: ConnectorEndpointRoleRepository,
        camelContext: CamelContext,
    ) = ConnectorService(
        connectorRepository,
        connectorInstanceRepository,
        connectorEndpointRepository,
        connectorEndpointRoleRepository,
        camelContext,
    )

    @Bean
    fun connectorLookupProcessor() = ConnectorLookupProcessor(connectorRepository)

    @Bean
    fun endpointRestRoutesBuilder(connectorLookupProcessor: ConnectorLookupProcessor) = EndpointRestRoutesBuilder(connectorLookupProcessor)

    @Bean
    fun endpointAuthRouteBuilder(
        connectorEndpointRepository: ConnectorEndpointRepository,
        connectorInstanceRepository: ConnectorInstanceRepository,
        connectorEndpointRoleRepository: ConnectorEndpointRoleRepository,
    ) = EndpointAuthRouteBuilder(
        connectorEndpointRepository,
        connectorInstanceRepository,
        connectorEndpointRoleRepository,
    )

    @Bean
    fun endpointValidationRouteBuilder(
        connectorEndpointRepository: ConnectorEndpointRepository,
        connectorInstanceRepository: ConnectorInstanceRepository,
    ) = EndpointValidationRouteBuilder(
        connectorEndpointRepository,
        connectorInstanceRepository,
    )

    @Bean
    fun connectorDispatcherRouteBuilder() = ConnectorDispatcherRouteBuilder()

    @Bean
    fun connectorConfigRouteBuilder(
        connectorInstanceRepository: ConnectorInstanceRepository,
    ) = ConnectorConfigRouteBuilder(
        connectorInstanceRepository,
    )

    @Bean
    fun transformDispatcheRouteBuilder() = TransformDispatcheRouteBuilder()

    @Bean
    fun routeDependencyService(
        connectorInstanceRepository: ConnectorInstanceRepository,
        aggregatedDataProfileRepository: AggregatedDataProfileRepository,
        camelContext: CamelContext,
    ) = RouteDependencyService(connectorInstanceRepository, aggregatedDataProfileRepository, camelContext)

    @EventListener(ApplicationReadyEvent::class)
    @Order(1)
    fun loadAllConnectorsAtStartup(event: ApplicationReadyEvent) {
        val connectorService = event.applicationContext.getBean(ConnectorService::class.java)
        // Only load active connectors at startup
        connectorRepository.findAllByIsActiveTrue().forEach { connector ->
            try {
                connectorService.loadConnectorRoutes(connector)
                logger.debug { "Loaded routes for active connector: ${connector.tag} v${connector.version}" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to load connector ${connector.tag} v${connector.version}" }
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}