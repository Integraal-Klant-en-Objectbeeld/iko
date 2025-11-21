package com.ritense.iko.cache.processor

import com.ritense.iko.cache.service.CacheService
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
        if (cacheEnabled && !cacheHit) {
            // Make cache entry
            val cacheKey = exchange.getVariable("cacheKey", String::class.java)
            val cacheTTL = exchange.getVariable("cacheTTL", Int::class.java)
            val cacheValue = exchange.message.getBody(InputStream::class.java).readBytes().toString(Charsets.UTF_8)
            cacheService.put(
                key = cacheKey,
                value = cacheValue,
                ttl = cacheTTL.milliseconds.toJavaDuration()
            )
        }
    }
}