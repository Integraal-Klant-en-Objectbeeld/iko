package com.ritense.iko.cache.processor

import com.ritense.iko.cache.service.CacheService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.Exchange
import org.apache.camel.Processor

class AdpCacheCheckProcessor(
    private val cacheService: CacheService
) : Processor {
    override fun process(exchange: Exchange) {
        val cacheEnabled = exchange.getVariable("cacheEnabled", Boolean::class.java)
        logger.debug { "ADP cache check: enabled=$cacheEnabled" }
        if (cacheEnabled) {
            val cacheKey = exchange.getVariable("cacheKey", String::class.java)
            logger.debug { "ADP cache check: looking up key='$cacheKey'" }
            val cached = cacheService.get(key = cacheKey)

            if (cached != null) {
                // Put cached value on the body so the rest of the route sees it
                exchange.message.body = cached
                exchange.setVariable("cacheHit", true)
                logger.debug { "ADP cache check: cache HIT for key='$cacheKey' (size=${cached.length})" }
                exchange.isRouteStop = true
            } else {
                exchange.setVariable("cacheHit", false)
                logger.debug { "ADP cache check: cache MISS for key='$cacheKey'" }
            }
        } else {
            logger.debug { "ADP cache check: caching disabled, skipping lookup" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}