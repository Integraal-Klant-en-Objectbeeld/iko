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
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.CamelContext
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
internal class AggregatedDataProfileService(
    private val camelContext: CamelContext,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val ikoCacheProcessor: CacheProcessor,
) {
    private val logger = KotlinLogging.logger {}

    @EventListener(ApplicationReadyEvent::class)
    fun loadAllAggregatedDataProfilesAtStartup(event: ApplicationReadyEvent) {
        // Only load active profiles at startup
        aggregatedDataProfileRepository.findAllByIsActiveTrue().forEach { aggregatedDataProfile ->
            val adpRoute = AggregatedDataProfileRouteBuilder(
                camelContext,
                aggregatedDataProfile,
                connectorInstanceRepository,
                connectorEndpointRepository,
                ikoCacheProcessor,
            )
            camelContext.addRoutes(adpRoute)
            logger.debug { "Loaded routes for active ADP: ${aggregatedDataProfile.name} v${aggregatedDataProfile.version}" }
        }
    }

    fun removeRoutes(aggregatedDataProfile: AggregatedDataProfile) {
        val groupName = "adp_${aggregatedDataProfile.id}"
        camelContext.getRoutesByGroup(groupName).forEach { route ->
            camelContext.routeController.stopRoute(route.id)
            camelContext.removeRoute(route.id)
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

    /**
     * Activates a specific version of an AggregatedDataProfile.
     * Deactivates any currently active version and loads routes for the new active version.
     */
    @Transactional
    fun activateVersion(id: UUID) {
        val profileToActivate = aggregatedDataProfileRepository.findById(id)
            .orElseThrow { NoSuchElementException("AggregatedDataProfile not found: $id") }

        // Prevent reactivation of already active version
        if (profileToActivate.isActive) {
            logger.debug { "ADP ${profileToActivate.name} v${profileToActivate.version} is already active" }
            return
        }

        // Find and deactivate currently active version
        val currentActive = aggregatedDataProfileRepository.findByNameAndIsActiveTrue(profileToActivate.name)
        if (currentActive != null) {
            logger.debug { "Deactivating ADP ${currentActive.name} v${currentActive.version}" }
            removeRoutes(currentActive)
            currentActive.isActive = false
            aggregatedDataProfileRepository.save(currentActive)
        }

        // Activate new version
        profileToActivate.isActive = true
        aggregatedDataProfileRepository.save(profileToActivate)
        addRoutes(profileToActivate)
        logger.debug { "Activated ADP ${profileToActivate.name} v${profileToActivate.version}" }
    }

    /**
     * Creates a new version of an AggregatedDataProfile by copying it and all its relations.
     * The new version starts as inactive.
     */
    @Transactional
    fun createNewVersion(sourceId: UUID, newVersion: String): AggregatedDataProfile {
        val source = aggregatedDataProfileRepository.findById(sourceId)
            .orElseThrow { NoSuchElementException("AggregatedDataProfile not found: $sourceId") }

        // Check version doesn't already exist
        if (aggregatedDataProfileRepository.findByNameAndVersion(source.name, newVersion) != null) {
            throw IllegalArgumentException("Version $newVersion already exists for ${source.name}")
        }

        // Create new ADP (without relations)
        val newAdp = source.createNewVersion(newVersion)
        aggregatedDataProfileRepository.save(newAdp)

        // Build ID mapping: old ID -> new ID
        // Include the ADP ID mapping for level 1 relations
        val idMapping = mutableMapOf<UUID, UUID>()
        idMapping[source.id] = newAdp.id

        // First pass: copy all relations and build ID mapping
        val newRelations = source.relations.map { oldRelation ->
            val newRelation = oldRelation.copyForNewVersion(newAdp)
            idMapping[oldRelation.id] = newRelation.id
            oldRelation to newRelation
        }

        // Second pass: remap sourceId using the ID mapping
        newRelations.forEach { (oldRelation, newRelation) ->
            // sourceId points to either the ADP or a parent relation
            // Use the mapping to get the new ID
            newRelation.sourceId = oldRelation.sourceId?.let { idMapping[it] }
        }

        // Add all copied relations to the new ADP
        newAdp.relations.addAll(newRelations.map { it.second })
        logger.debug { "Created new version ${newAdp.name} v${newAdp.version} with ${newAdp.relations.size} relations" }

        return aggregatedDataProfileRepository.save(newAdp)
    }
}