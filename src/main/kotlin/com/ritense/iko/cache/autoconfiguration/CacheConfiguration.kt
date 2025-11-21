package com.ritense.iko.cache.autoconfiguration

import com.ritense.iko.cache.processor.AdpCacheCheckProcessor
import com.ritense.iko.cache.processor.AdpCachePutProcessor
import com.ritense.iko.cache.service.CacheService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CacheConfiguration {

    @Bean
    fun cacheService() = CacheService()

    @Bean
    fun adpCacheCheckProcessor(cacheService: CacheService) = AdpCacheCheckProcessor(cacheService)

    @Bean
    fun adpCachePutProcessor(cacheService: CacheService) = AdpCachePutProcessor(cacheService)

}