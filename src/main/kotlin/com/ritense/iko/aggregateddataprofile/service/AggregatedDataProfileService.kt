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
import com.ritense.iko.aggregateddataprofile.domain.EntityStatus
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.processor.CacheProcessor
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import com.ritense.iko.connectors.service.ConnectorService
import com.ritense.iko.connectors.service.RouteDependencyService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.CamelContext
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

open class AggregatedDataProfileService(
    private val camelContext: CamelContext,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorService: ConnectorService,
    private val routeDependencyService: RouteDependencyService,
    private val ikoCacheProcessor: CacheProcessor,
) {

    @EventListener(ApplicationReadyEvent::class)
    @Order(2)
    open fun loadAllAggregatedDataProfilesAtStartup(event: ApplicationReadyEvent) {
        aggregatedDataProfileRepository.findAllByIsActiveTrue().forEach { aggregatedDataProfile ->
            loadRoute(aggregatedDataProfile)
            logger.debug { "Loaded routes for active ADP: ${aggregatedDataProfile.name} v${aggregatedDataProfile.version}" }
        }
    }

    open fun removeRoute(aggregatedDataProfile: AggregatedDataProfile) {
        val connectorDeps = routeDependencyService.resolveConnectorDependencies(aggregatedDataProfile)

        val groupName = "group:adp:${aggregatedDataProfile.id}"
        camelContext.getRoutesByGroup(groupName).forEach { route ->
            camelContext.routeController.stopRoute(route.id)
            camelContext.removeRoute(route.id)
        }

        for (connector in connectorDeps) {
            if (!routeDependencyService.isConnectorRouteNeeded(connector)) {
                connectorService.removeConnectorRoutes(connector)
            }
        }
    }

    open fun loadRoute(aggregatedDataProfile: AggregatedDataProfile) {
        val requiredConnectors = routeDependencyService.resolveConnectorDependencies(aggregatedDataProfile)
        for (connector in requiredConnectors) {
            if (!routeDependencyService.isConnectorRouteLoaded(connector)) {
                connectorService.loadConnectorRoutes(connector)
            }
        }
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

    open fun reloadRoute(aggregatedDataProfile: AggregatedDataProfile) {
        removeRoute(aggregatedDataProfile)
        loadRoute(aggregatedDataProfile)
    }

    /**
     * Activates a specific version of an AggregatedDataProfile.
     * Deactivates any currently active version and loads routes for the new active version.
     */
    @Transactional
    open fun activateVersion(id: UUID) {
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
            removeRoute(currentActive)
            currentActive.isActive = false
            aggregatedDataProfileRepository.saveAndFlush(currentActive)
        }

        // Activate new version
        profileToActivate.isActive = true
        aggregatedDataProfileRepository.save(profileToActivate)

        try {
            loadRoute(profileToActivate)
        } catch (e: Exception) {
            // Recovery: restore old version if new route loading fails
            if (currentActive != null) {
                try {
                    currentActive.isActive = true
                    aggregatedDataProfileRepository.saveAndFlush(currentActive)
                    profileToActivate.isActive = false
                    aggregatedDataProfileRepository.save(profileToActivate)
                    loadRoute(currentActive)
                } catch (recoveryEx: Exception) {
                    logger.error(recoveryEx) { "Failed to recover old ADP route: ${currentActive.name} v${currentActive.version}" }
                }
            }
            throw e
        }
        logger.debug { "Activated ADP ${profileToActivate.name} v${profileToActivate.version}" }
    }

    /**
     * Creates a new version of an AggregatedDataProfile by copying it and all its relations.
     * The new version starts as inactive.
     */
    @Transactional
    open fun createNewVersion(sourceId: UUID, newVersion: String): AggregatedDataProfile {
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

    open fun previewFinalization(id: UUID): FinalizationImpact {
        val adp = aggregatedDataProfileRepository.findById(id)
            .orElseThrow { NoSuchElementException("Profile not found: $id") }
        require(adp.status == EntityStatus.DRAFT) { "Already finalized" }

        val allInstanceIds = buildSet {
            add(adp.connectorInstanceId)
            adp.relations.forEach { add(it.connectorInstanceId) }
        }

        val draftConnectors = mutableListOf<com.ritense.iko.connectors.domain.Connector>()
        val errors = mutableListOf<String>()

        for (instanceId in allInstanceIds) {
            val instance = connectorInstanceRepository.findById(instanceId).orElse(null)
            if (instance == null) {
                errors.add("Connector instance $instanceId not found")
                continue
            }
            val connector = instance.connector
            if (connector.status == EntityStatus.DRAFT) {
                draftConnectors.add(connector)
            }
        }

        val connectorImpacts = draftConnectors.distinctBy { it.id }.map { connector ->
            val instances = connectorInstanceRepository.findByConnector(connector)
            val instanceIds = instances.map { it.id }.toSet()

            val allDraftAdps = aggregatedDataProfileRepository.findAllByStatus(EntityStatus.DRAFT)
            val affectedAdps = allDraftAdps
                .filter { it.id != adp.id }
                .filter { otherAdp ->
                    val otherInstanceIds = buildSet {
                        add(otherAdp.connectorInstanceId)
                        otherAdp.relations.forEach { add(it.connectorInstanceId) }
                    }
                    otherInstanceIds.any { it in instanceIds }
                }
                .map { AffectedAdp(it.id, it.name, it.version.value) }

            try {
                connectorService.validateConnectorCode(connector.connectorCode, connector.tag)
            } catch (e: Exception) {
                errors.add("Connector '${connector.tag}' v${connector.version} has invalid code: ${e.message}")
            }
            val endpoints = connectorEndpointRepository.findByConnector(connector)
            if (endpoints.isEmpty()) {
                errors.add("Connector '${connector.tag}' v${connector.version} has no endpoints")
            }

            ConnectorImpact(
                connectorId = connector.id,
                connectorName = connector.name,
                connectorTag = connector.tag,
                connectorVersion = connector.version.value,
                affectedDraftAdps = affectedAdps,
            )
        }

        return FinalizationImpact(
            adpId = adp.id,
            adpName = adp.name,
            adpVersion = adp.version.value,
            connectorsToFinalize = connectorImpacts,
            canFinalize = errors.isEmpty(),
            errors = errors,
        )
    }

    @Transactional
    open fun finalizeProfile(id: UUID): AggregatedDataProfile {
        val adp = aggregatedDataProfileRepository.findById(id)
            .orElseThrow { NoSuchElementException("Profile not found: $id") }
        require(adp.status == EntityStatus.DRAFT) { "Already finalized" }

        val allInstanceIds = buildSet {
            add(adp.connectorInstanceId)
            adp.relations.forEach { add(it.connectorInstanceId) }
        }

        for (instanceId in allInstanceIds) {
            val instance = connectorInstanceRepository.findById(instanceId)
                .orElseThrow { NoSuchElementException("Connector instance $instanceId not found") }
            val connector = instance.connector
            if (connector.status == EntityStatus.DRAFT) {
                connectorService.finalizeConnector(connector.id)
            }
        }

        adp.finalize()
        return aggregatedDataProfileRepository.save(adp)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}