package com.ritense.iko.cache.domain

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.HIT
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.MISS
import org.apache.camel.Exchange


interface Cacheable {
    val id: String
    val cacheKey: String
    val cacheSettings: CacheSettings
    fun handleCacheEvent(exchange: Exchange, cacheEvent: CacheEntry)
}

fun AggregatedDataProfile.toCacheable(): Cacheable {
    val id = this.id
    val cacheSettings = this.aggregatedDataProfileCacheSetting
    val cacheKey =
        listOf(
            id.toString(),
            transform.expression,
//            TODO: Add filters and sort params
        ).joinToString(separator = "")

    return object : Cacheable {
        override val id = id.toString()
        override val cacheKey: String = cacheKey
        override val cacheSettings = object : CacheSettings {
            override val enabled = cacheSettings.enabled
            override val timeToLive = cacheSettings.timeToLive
        }

        override fun handleCacheEvent(exchange: Exchange, cacheEvent: CacheEntry) {
            if (cacheEvent.type == HIT) {
                with(exchange) {
                    message.body = cacheEvent.value
                    isRouteStop = true
                }
            }
        }
    }
}

fun Relation.toCacheable(): Cacheable {
    val id = this.id.toString()
    val cacheSettings = this.relationCacheSettings
    val cacheKey =
        listOf(
            id,
            sourceToEndpointMapping,
            transform.expression,
//            TODO: Add filters and sort params
        ).joinToString(separator = "")

    return object : Cacheable {
        override val id = id
        override val cacheKey: String = cacheKey
        override val cacheSettings = object : CacheSettings {
            override val enabled = cacheSettings.enabled
            override val timeToLive = cacheSettings.timeToLive
        }

        override fun handleCacheEvent(exchange: Exchange, cacheEvent: CacheEntry) =
            with(exchange) {
                if (cacheEvent.type == HIT) {
                    message.body = cacheEvent.value
                    exchange.setVariable("cacheHit_$id", true)
                } else if (cacheEvent.type == MISS) {
                    exchange.setVariable("cacheHit_$id", false)
                }
            }

    }
}