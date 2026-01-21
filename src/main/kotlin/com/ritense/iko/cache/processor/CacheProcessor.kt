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

package com.ritense.iko.cache.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.cache.domain.CacheEntry
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.HIT
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.MISS
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.PUT
import com.ritense.iko.cache.domain.Cacheable
import com.ritense.iko.cache.service.CacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.Exchange
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class CacheProcessor(
    private val cacheService: CacheService,
    private val objectMapper: ObjectMapper,
) {
    fun checkCache(
        exchange: Exchange,
        cacheable: Cacheable,
    ) = with(cacheable) {
        if (!cacheSettings.enabled) {
            logger.debug { "Cache is disabled for Cacheable with Id: $id" }
            return
        }
        val cacheKey = "$id:" + cacheService.hashString(cacheKey(exchange))
        val cacheEvent = checkCache(cacheKey)

        handleCacheEntry(
            exchange = exchange,
            cacheEvent = cacheEvent,
        )
    }

    fun putCache(
        exchange: Exchange,
        cacheable: Cacheable,
    ): CacheEntry? = with(cacheable) {
        if (!cacheSettings.enabled) {
            logger.debug { "Cache is disabled for Cacheable with Id: $id" }
            return null
        }
        val cacheKey = "$id:${cacheService.hashString(cacheKey(exchange))}"

        putCache(
            key = cacheKey,
            value = objectMapper.writeValueAsString(exchange.message.body),
            timeToLive = cacheSettings.timeToLive.milliseconds.toJavaDuration(),
        )
    }

    private fun checkCache(cacheKey: String): CacheEntry = when (val cacheValue = cacheService.get(key = cacheKey)) {
        null ->
            CacheEntry(type = MISS, key = cacheKey)
                .also {
                    logCacheEntry(event = it)
                }

        else ->
            CacheEntry(type = HIT, key = cacheKey, value = cacheValue)
                .also {
                    logCacheEntry(
                        event = it,
                        extraMessage = "Cache value size: '${cacheValue.length}'",
                    )
                }
    }

    private fun putCache(
        key: String,
        value: String,
        timeToLive: Duration? = null,
    ): CacheEntry {
        cacheService
            .put(
                key = key,
                value = value,
                ttl = timeToLive,
            )

        return CacheEntry(type = PUT, key = key, value = value)
            .also {
                logCacheEntry(
                    event = it,
                    extraMessage = "Cache value size: '${it.value?.length ?: "0"}'. TTL value: '${it.timeToLive ?: "0"}'. ",
                )
            }
    }

    private fun logCacheEntry(
        event: CacheEntry,
        extraMessage: String? = null,
    ) {
        logger.debug { "Cache [${event.type}] for key: '${event.key}'. ${extraMessage ?: ""}" }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}