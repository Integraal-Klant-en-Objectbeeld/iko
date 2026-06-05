/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
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

import com.ritense.iko.cache.service.CacheService
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_INSTANCE_ID_VARIABLE
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.Exchange
import java.time.Duration
import java.util.UUID

class TokenCacheProcessor(
    private val cacheService: CacheService,
) {
    fun lookup(exchange: Exchange) {
        val key = buildCacheKey(exchange)
        val cached = cacheService.get(key)
        if (cached != null) {
            exchange.setVariable(ACCESS_TOKEN_VARIABLE, cached)
            logger.debug { "Token cache HIT key='$key'" }
        } else {
            logger.debug { "Token cache MISS key='$key'" }
        }
    }

    fun store(exchange: Exchange) {
        val key = buildCacheKey(exchange)

        @Suppress("UNCHECKED_CAST")
        val body = exchange.message.getBody(Map::class.java) as? Map<String, Any?>
            ?: error("token-exchange response body was not a Map")
        val accessToken = body["access_token"] as? String
            ?: error("token-exchange response missing access_token")
        val expiresIn = (body["expires_in"] as? Number)?.toLong() ?: 0L
        require(expiresIn > 0) { "token-exchange response missing or zero expires_in" }

        val ttlSeconds = (expiresIn * SAFETY_FACTOR).toLong().coerceAtLeast(1)
        cacheService.put(key, accessToken, Duration.ofSeconds(ttlSeconds))
        exchange.setVariable(ACCESS_TOKEN_VARIABLE, accessToken)
        logger.debug { "Token cache PUT key='$key' ttlSec='$ttlSeconds'" }
    }

    private fun buildCacheKey(exchange: Exchange): String {
        val instanceId = exchange.getVariable(CONNECTOR_INSTANCE_ID_VARIABLE, UUID::class.java)
            ?: error("$CONNECTOR_INSTANCE_ID_VARIABLE variable not set; ensure direct:iko:endpoint:validate ran before this step")
        return "token:keycloak:$instanceId"
    }

    companion object {
        const val ACCESS_TOKEN_VARIABLE = "accessToken"
        private const val SAFETY_FACTOR = 0.9
        private val logger = KotlinLogging.logger {}
    }
}