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

package com.ritense.iko.aggregateddataprofile.autoconfiguration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRoute
import com.ritense.iko.aggregateddataprofile.camel.AggregatedDataProfileRouteBuilder
import com.ritense.iko.aggregateddataprofile.processor.ContainerParamsProcessor
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.processor.CacheProcessor
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AggregatedDataProfileConfiguration(
    private val camelContext: CamelContext,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val cacheProcessor: CacheProcessor,
) {
    init {
        this.aggregatedDataProfileRepository.findAll().forEach { aggregatedDataProfile ->
            val adpRoute = AggregatedDataProfileRouteBuilder(
                camelContext,
                aggregatedDataProfile,
                connectorInstanceRepository,
                connectorEndpointRepository,
                cacheProcessor,
            )
            camelContext.addRoutes(adpRoute)
        }
    }

    @Bean
    fun aggregatedDataProfileRoute(
        containerParamsProcessor: ContainerParamsProcessor,
    ) = AggregatedDataProfileRoute(
        aggregatedDataProfileRepository,
        containerParamsProcessor,
    )

    @Bean
    fun containerParamsProcessor(
        objectMapper: ObjectMapper,
    ): ContainerParamsProcessor = ContainerParamsProcessor(objectMapper)
}