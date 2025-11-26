package com.ritense.iko.cache.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.service.CacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.Exchange
import org.apache.camel.Processor
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

// TODO Set var scoping issue
// TODO redundacy of code
class RelationCachePutProcessor(
    private val cacheService: CacheService,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val objectMapper: ObjectMapper
) : Processor {
    override fun process(exchange: Exchange) {
        val adpId = UUID.fromString(exchange.getVariable("aggregatedDataProfileId").toString())
        val adp = aggregatedDataProfileRepository.getReferenceById(adpId)

        val relationId = UUID.fromString(exchange.getVariable("relationId").toString())
        val relation = adp.relations.first { it.id == relationId}

        val cacheEnabled = relation.aggregatedDataProfileCacheSetting.enabled
        if (cacheEnabled) {
            // Make cache entry
            val cacheKey = cacheService.createKey(relation.id.toString(), exchange.getVariable("endpointMapping").toString())
            val cacheTTL = relation.aggregatedDataProfileCacheSetting.timeToLive
            val cacheValue = objectMapper.writeValueAsString(exchange.message.body)
            logger.debug { "Relation cache put: storing key='$cacheKey' ttlMs='${cacheTTL}' valueSize='${cacheValue.length}'" }
            cacheService.put(
                key = cacheKey,
                value = cacheValue,
                ttl = cacheTTL.milliseconds.toJavaDuration()
            )
        } else {
            logger.debug { "Relation cache put: caching disabled, skipping store" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}