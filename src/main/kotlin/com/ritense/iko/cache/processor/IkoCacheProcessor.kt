package com.ritense.iko.cache.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.cache.domain.IkoCacheEvent
import com.ritense.iko.cache.domain.IkoCacheEvent.HIT
import com.ritense.iko.cache.domain.IkoCacheEvent.MISS
import com.ritense.iko.cache.domain.IkoCacheEvent.PUT
import com.ritense.iko.cache.service.CacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.Exchange
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class IkoCacheProcessor(private val cacheService: CacheService, private val objectMapper: ObjectMapper) {

    fun checkCache(exchange: Exchange, entity: Any, variance: Any = ""): Exchange {
        when (entity) {
            is AggregatedDataProfile -> {
                if (!entity.aggregatedDataProfileCacheSetting.enabled) {
                    logger.debug { "Cache is disabled for ${entity.javaClass.simpleName} with Id: ${entity.id}" }
                    return exchange
                }
                val cacheKey = getAdpCacheKey(entity, variance)
                val (cacheHit, cacheValue) = checkCache(cacheKey)

                if (cacheHit) {
                    with(exchange) {
                        message.body = cacheValue
                        isRouteStop = true
                    }
                }
            }

            is Relation -> {
                if (!entity.aggregatedDataProfileCacheSetting.enabled) {
                    logger.debug { "Cache is disabled for ${entity.javaClass.simpleName} with Id: ${entity.id}" }
                    return exchange
                }
                val cacheKey = getRelationCacheKey(entity, variance)

                val (cacheHit, cacheValue) = checkCache(cacheKey)
                with(exchange) {
                    if (cacheHit) message.body = cacheValue
                    exchange.setVariable("cacheHit_${entity.id}", cacheHit)
                }
            }

            else -> {
                logger.debug { "${entity.javaClass.simpleName} isn't cacheable" }
            }
        }

        return exchange
    }

    fun putCache(exchange: Exchange, entity: Any, variance: Any = ""): Exchange {
        when (entity) {
            is AggregatedDataProfile -> {
                if (!entity.aggregatedDataProfileCacheSetting.enabled) {
                    logger.debug { "Cache is disabled for ${entity.javaClass.simpleName} with Id: ${entity.id}" }
                    return exchange
                }
                val cacheKey = getAdpCacheKey(entity, variance)
                val cacheValue = objectMapper.writeValueAsString(exchange.message.body)
                val cacheTTL = entity.aggregatedDataProfileCacheSetting.timeToLive.milliseconds.toJavaDuration()

                putCache(cacheKey, cacheValue, cacheTTL)
            }

            is Relation -> {
                if (!entity.aggregatedDataProfileCacheSetting.enabled) {
                    logger.debug { "Cache is disabled for ${entity.javaClass.simpleName} with Id: ${entity.id}" }
                    return exchange
                }
                val cacheKey = getRelationCacheKey(entity, variance)
                val cacheValue = objectMapper.writeValueAsString(exchange.message.body)
                val cacheTTL = entity.aggregatedDataProfileCacheSetting.timeToLive.milliseconds.toJavaDuration()

                putCache(cacheKey, cacheValue, cacheTTL)
            }

            else -> {
                logger.debug { "${entity.javaClass.simpleName} isn't cacheable" }
            }
        }

        return exchange
    }

    private fun getAdpCacheKey(entity: AggregatedDataProfile, variance: Any) =
        computeCacheKey(
            entity.id.toString(),
            entity.transform.expression,
            objectMapper.writeValueAsString(variance),
//            TODO: Add filters and sort params
        )

    private fun getRelationCacheKey(entity: Relation, variance: Any) =
        computeCacheKey(
            entity.id.toString(),
            entity.sourceToEndpointMapping,
            entity.transform.expression,
            objectMapper.writeValueAsString(variance),
//            TODO: Add filters and sort params
        )

    private fun computeCacheKey(vararg keyParts: String): String =
        cacheService.hashString(keyParts.joinToString(separator = ""))

    private fun checkCache(cacheKey: String): Pair<Boolean, String?> {
        return when (val cacheHit = cacheService.get(key = cacheKey)) {
            null -> {
                logCacheEvent(event = MISS, cacheKey = cacheKey)
                Pair(false, null)
            }

            else -> {
                logCacheEvent(event = HIT, cacheKey = cacheKey, extraMessage = "Cache value size: '${cacheHit.length}'")
                Pair(true, cacheHit)
            }
        }
    }

    private fun putCache(cacheKey: String, cacheValue: String, cacheTTL: Duration) {
        cacheService
            .put(
                key = cacheKey,
                value = cacheValue,
                ttl = cacheTTL
            )
        logCacheEvent(
            event = PUT,
            cacheKey = cacheKey,
            extraMessage = "TTL value: '$cacheTTL'. Cache value size: '${cacheValue.length}'"
        )
    }

    private fun logCacheEvent(event: IkoCacheEvent, cacheKey: String, extraMessage: String? = null) {
        logger.debug { "Cache [${event.name}] for key: '$cacheKey'. ${extraMessage ?: ""}" }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
