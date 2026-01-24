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

package com.ritense.iko.cache.domain

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.HIT
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.MISS
import com.ritense.iko.camel.IkoConstants.Variables.ENDPOINT_TRANSFORM_RESULT_VARIABLE
import org.apache.camel.Exchange
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

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
            val endpointMappingResult = exchange.getVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, String::class.java)
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
                    // Ensure downstream components interpret the cached payload as JSON
                    message.setHeader(Exchange.CONTENT_TYPE, APPLICATION_JSON_VALUE)
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
            val endpointMappingResult = exchange.getVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, String::class.java)

            listOf(
                id,
                endpointTransform,
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
                // Ensure downstream components interpret the cached payload as JSON
                message.setHeader(Exchange.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                exchange.setVariable("cacheHit_$id", true)
            } else if (cacheEvent.type == MISS) {
                exchange.setVariable("cacheHit_$id", false)
            }
        }
    }
}