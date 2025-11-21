package com.ritense.iko.cache.processor

import com.ritense.iko.cache.service.CacheService
import org.apache.camel.Exchange
import org.apache.camel.Processor

class AdpCacheCheckProcessor(
    private val cacheService: CacheService
) : Processor {
    override fun process(exchange: Exchange) {
        val cacheEnabled = exchange.getVariable("cacheEnabled", Boolean::class.java)
        if (cacheEnabled) {
            val cacheKey = exchange.getVariable("cacheKey", String::class.java)
            val cached = cacheService.get(key = cacheKey)

            if (cached != null) {
                // Put cached value on the body so the rest of the route sees it
                exchange.message.body = cached
                exchange.setVariable("cacheHit", true)
            } else {
                exchange.setVariable("cacheHit", false)
            }
        }
    }
}