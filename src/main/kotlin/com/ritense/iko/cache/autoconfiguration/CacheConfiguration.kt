package com.ritense.iko.cache.autoconfiguration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.cache.processor.CacheProcessor
import com.ritense.iko.cache.service.CacheService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class CacheConfiguration {
    @Bean
    fun cacheService(stringRedisTemplate: StringRedisTemplate) = CacheService(stringRedisTemplate)

    @Bean
    fun cacheProcessor(
        cacheService: CacheService,
        objectMapper: ObjectMapper,
    ): CacheProcessor = CacheProcessor(cacheService, objectMapper)
}