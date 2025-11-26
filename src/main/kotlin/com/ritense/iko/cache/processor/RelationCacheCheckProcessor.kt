package com.ritense.iko.cache.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.service.CacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.Exchange
import org.apache.camel.Processor
import java.util.UUID

// TODO Set var scoping issue
// TODO redundacy of code
class RelationCacheCheckProcessor(
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
        logger.debug { "Relation cache check: enabled=$cacheEnabled" }
        if (cacheEnabled) {
            val cacheKey = cacheService.createKey(relation.id.toString(), exchange.getVariable("endpointMapping").toString())
            logger.debug { "Relation cache check: looking up key='$cacheKey'" }
            val cached = cacheService.get(key = cacheKey)

            if (cached != null) {
                // Put cached value on the body so the rest of the route sees it
                exchange.message.body = objectMapper.readValue(cached, Map::class.java)
                exchange.setVariable("cacheHit_$relationId", true)
                logger.debug { "Relation cache check: cache HIT for key='$cacheKey' (size=${cached.length})" }
            } else {
                exchange.setVariable("cacheHit_$relationId", false)
                logger.debug { "Relation cache check: cache MISS for key='$cacheKey'" }
            }
        } else {
            logger.debug { "Relation cache check: caching disabled, skipping lookup" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}