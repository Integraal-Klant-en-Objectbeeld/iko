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

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.connectors.domain.Connector
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.springframework.stereotype.Service

@Service
internal class RouteDependencyService(
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val camelContext: CamelContext,
) {
    fun resolveConnectorDependencies(adp: AggregatedDataProfile): Set<Connector> {
        val allInstanceIds = buildSet {
            add(adp.connectorInstanceId)
            adp.relations.forEach { add(it.connectorInstanceId) }
        }
        return allInstanceIds.mapNotNull { instanceId ->
            connectorInstanceRepository.findById(instanceId).orElse(null)?.connector
        }.toSet()
    }

    fun isConnectorRouteLoaded(connector: Connector): Boolean = camelContext.getRoutesByGroup("connector_${connector.id}").isNotEmpty()

    fun isConnectorRouteNeeded(connector: Connector): Boolean {
        if (connector.isActive) return true

        val instances = connectorInstanceRepository.findByConnector(connector)
        if (instances.isEmpty()) return false
        val instanceIds = instances.map { it.id }.toSet()

        return aggregatedDataProfileRepository.findAllByIsActiveTrue().any { adp ->
            val adpInstanceIds = buildSet {
                add(adp.connectorInstanceId)
                adp.relations.forEach { add(it.connectorInstanceId) }
            }
            adpInstanceIds.any { it in instanceIds }
        }
    }
}