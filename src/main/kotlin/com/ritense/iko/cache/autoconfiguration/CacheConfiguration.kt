package com.ritense.iko.cache.autoconfiguration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.cache.processor.IkoCacheProcessor
import com.ritense.iko.cache.service.CacheService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
class CacheConfiguration {

    @Bean
    fun cacheService(stringRedisTemplate: StringRedisTemplate) =
        CacheService(stringRedisTemplate)

    @Bean
    fun ikoCacheProcessor(cacheService: CacheService, objectMapper: ObjectMapper): IkoCacheProcessor =
        IkoCacheProcessor(cacheService, objectMapper)
}