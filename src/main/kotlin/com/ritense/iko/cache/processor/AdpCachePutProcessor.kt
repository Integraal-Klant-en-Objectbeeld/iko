package com.ritense.iko.cache.processor

import com.ritense.iko.cache.service.CacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.Exchange
import org.apache.camel.Processor
import java.io.InputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class AdpCachePutProcessor(
    private val cacheService: CacheService
) : Processor {
    override fun process(exchange: Exchange) {
        val cacheEnabled = exchange.getVariable("cacheEnabled", Boolean::class.java)
        val cacheHit = exchange.getVariable("cacheHit", Boolean::class.java)
        logger.debug { "ADP cache put: enabled=$cacheEnabled, cacheHit=$cacheHit" }
        if (cacheEnabled && !cacheHit) {
            // Make cache entry
            val cacheKey = exchange.getVariable("cacheKey", String::class.java)
            val cacheTTL = exchange.getVariable("cacheTTL", Int::class.java)
            val cacheValue = exchange.message.getBody(InputStream::class.java).readBytes().toString(Charsets.UTF_8)
            logger.debug { "ADP cache put: storing key='$cacheKey' ttlMs='${cacheTTL}' valueSize='${cacheValue.length}'" }
            cacheService.put(
                key = cacheKey,
                value = cacheValue,
                ttl = cacheTTL.milliseconds.toJavaDuration()
            )
        } else if (!cacheEnabled) {
            logger.debug { "ADP cache put: caching disabled, skipping store" }
        } else if (cacheHit) {
            logger.debug { "ADP cache put: cache hit already satisfied response, skipping store" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}