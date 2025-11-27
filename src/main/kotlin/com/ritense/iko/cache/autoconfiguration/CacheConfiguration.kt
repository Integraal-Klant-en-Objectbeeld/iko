package com.ritense.iko.cache.autoconfiguration

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.processor.AdpCacheCheckProcessor
import com.ritense.iko.cache.processor.AdpCachePutProcessor
import com.ritense.iko.cache.processor.IkoCacheProcessor
import com.ritense.iko.cache.processor.RelationCacheCheckProcessor
import com.ritense.iko.cache.processor.RelationCachePutProcessor
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