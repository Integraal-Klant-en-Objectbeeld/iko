package com.ritense.iko.cache.domain

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.HIT
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.MISS
import org.apache.camel.Exchange

interface Cacheable {
    val id: String
    val cacheSettings: CacheSettings
    val cacheKey: (Exchange) -> String

    fun handleCacheEntry(
        exchange: Exchange,
        cacheEvent: CacheEntry,
    )
}

fun AggregatedDataProfile.toCacheable(): Cacheable {
    val id = this.id
    val cacheSettings = this.aggregatedDataProfileCacheSetting

    return object : Cacheable {
        override val id = id.toString()
        override val cacheKey: (Exchange) -> String = { exchange ->
            val endpointMappingResult = exchange.getVariable("endpointTransformResult", "{}", String::class.java)
            listOf(
                id.toString(),
                endpointTransform.expression,
                endpointMappingResult,
                resultTransform.expression,
            ).joinToString(separator = "")
        }
        override val cacheSettings =
            object : CacheSettings {
                override val enabled = cacheSettings.enabled
                override val timeToLive = cacheSettings.timeToLive
            }

        override fun handleCacheEntry(
            exchange: Exchange,
            cacheEvent: CacheEntry,
        ) {
            if (cacheEvent.type == HIT) {
                with(exchange) {
                    message.body = cacheEvent.value
                    message.setHeader("Content-Type", "application/json")
                    isRouteStop = true
                }
            }
        }
    }
}

fun Relation.toCacheable(): Cacheable {
    val id = this.id.toString()
    val cacheSettings = this.relationCacheSettings

    return object : Cacheable {
        override val id = id
        override val cacheSettings =
            object : CacheSettings {
                override val enabled = cacheSettings.enabled
                override val timeToLive = cacheSettings.timeToLive
            }
        override val cacheKey: (Exchange) -> String = { exchange ->
            val endpointMappingResult = exchange.getVariable("endpointTransformResult", "{}", String::class.java)

            listOf(
                id,
                sourceToEndpointMapping,
                endpointMappingResult,
                resultTransform.expression,
            ).joinToString(separator = "")
        }

        override fun handleCacheEntry(
            exchange: Exchange,
            cacheEvent: CacheEntry,
        ) = with(exchange) {
            if (cacheEvent.type == HIT) {
                message.body = cacheEvent.value
                exchange.setVariable("cacheHit_$id", true)
            } else if (cacheEvent.type == MISS) {
                exchange.setVariable("cacheHit_$id", false)
            }
        }
    }
}