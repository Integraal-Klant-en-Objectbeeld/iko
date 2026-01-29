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

package com.ritense.iko.connectors.service

import com.ritense.iko.connectors.domain.Connector
import com.ritense.iko.connectors.repository.ConnectorRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.support.PluginHelper
import org.apache.camel.support.ResourceHelper
import org.springframework.stereotype.Service

@Service
class ConnectorService(
    private val connectorRepository: ConnectorRepository,
    private val camelContext: CamelContext,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Loads connector routes into CamelContext with group set for easy removal.
     * Uses Camel's RouteDefinition API instead of YAML string manipulation.
     *
     * Pattern: Parse YAML → Modify RouteDefinitions → Add via addRoutes()
     * This matches the existing AggregatedDataProfileService pattern.
     */
    fun loadConnectorRoutes(connector: Connector) {
        val groupName = "connector_${connector.id}"

        // Step 1: Create resource from YAML (filename must end in .yaml)
        val resource = ResourceHelper.fromString(
            "${connector.tag}.yaml",
            connector.connectorCode,
        )

        // Step 2: Parse YAML to RoutesBuilder objects WITHOUT loading into context
        val loader = PluginHelper.getRoutesLoader(camelContext)
        val builders = loader.findRoutesBuilders(listOf(resource))

        // Step 3: For each builder, configure it, modify route definitions, then add to context
        builders.forEach { builder ->
            val routeBuilder = builder as RouteBuilder
            routeBuilder.setCamelContext(camelContext)
            routeBuilder.configure()

            // Set group on each route definition BEFORE adding to context
            routeBuilder.routeCollection.routes.forEach { routeDef ->
                routeDef.group(groupName)
            }

            // Add routes using standard CamelContext.addRoutes() - same pattern as AggregatedDataProfileService
            camelContext.addRoutes(routeBuilder)
        }

        logger.debug { "Loaded ${builders.size} route builder(s) for connector ${connector.tag} with group $groupName" }
    }

    /**
     * Validates connector YAML by attempting to parse it.
     * Returns true if valid, throws exception with details if invalid.
     */
    fun validateConnectorCode(connectorCode: String, tag: String): Boolean {
        val resource = ResourceHelper.fromString("$tag.yaml", connectorCode)
        val loader = PluginHelper.getRoutesLoader(camelContext)

        // This will throw if YAML is invalid
        val builders = loader.findRoutesBuilders(listOf(resource))

        // Validate by calling configure() without adding to context
        builders.forEach { builder ->
            val routeBuilder = builder as RouteBuilder
            routeBuilder.setCamelContext(camelContext)
            routeBuilder.configure()
        }

        return true
    }

    /**
     * Removes all routes belonging to a connector using route groups.
     */
    fun removeConnectorRoutes(connector: Connector) {
        val groupName = "connector_${connector.id}"
        camelContext.getRoutesByGroup(groupName).forEach { route ->
            camelContext.routeController.stopRoute(route.id)
            camelContext.removeRoute(route.id)
        }
        logger.debug { "Removed routes for connector ${connector.tag} (group: $groupName)" }
    }
}