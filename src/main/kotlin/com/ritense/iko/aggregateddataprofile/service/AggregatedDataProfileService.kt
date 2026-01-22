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

package com.ritense.iko.aggregateddataprofile.service

import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRouteBuilder
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.processor.CacheProcessor
import com.ritense.iko.camel.GlobalErrorHandlerConfiguration
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener

class AggregatedDataProfileService(
    private val camelContext: CamelContext,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val ikoCacheProcessor: CacheProcessor,
    private val globalErrorHandlerConfiguration: GlobalErrorHandlerConfiguration,
) {

    @EventListener(
        ApplicationReadyEvent::class,
    )
    fun loadAllAggregatedDataProfilesAtStartup(
        event: ApplicationReadyEvent,
    ) {
        camelContext.addRoutes(globalErrorHandlerConfiguration)
        aggregatedDataProfileRepository.findAll().forEach { aggregatedDataProfile ->
            val adpRoute = AggregatedDataProfileRouteBuilder(
                camelContext,
                aggregatedDataProfile,
                connectorInstanceRepository,
                connectorEndpointRepository,
                ikoCacheProcessor,
            )
            camelContext.addRoutes(adpRoute)
        }
    }

    fun removeRoutes(aggregatedDataProfile: AggregatedDataProfile) {
        removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_direct")
        removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")

        aggregatedDataProfile.relations.forEach { relation ->
            removeRoute("relation_${relation.id}_direct")
            removeRoute("relation_${relation.id}_multicast")
        }
    }

    fun addRoutes(aggregatedDataProfile: AggregatedDataProfile) {
        camelContext.addRoutes(
            AggregatedDataProfileRouteBuilder(
                camelContext,
                aggregatedDataProfile,
                connectorEndpointRepository = connectorEndpointRepository,
                connectorInstanceRepository = connectorInstanceRepository,
                cacheProcessor = ikoCacheProcessor,
            ),
        )
    }

    fun reloadRoutes(aggregatedDataProfile: AggregatedDataProfile) {
        removeRoutes(aggregatedDataProfile)
        addRoutes(aggregatedDataProfile)
    }

    private fun removeRoute(id: String) {
        camelContext.routeController.stopRoute(id)
        camelContext.removeRoute(id)
    }
}