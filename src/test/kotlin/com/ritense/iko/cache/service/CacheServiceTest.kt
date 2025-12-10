package com.ritense.iko.cache.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.data.redis.core.StringRedisTemplate

class CacheServiceTest {

    @Test
    fun hashString() {
        val stringTemplate: StringRedisTemplate = mock()
        val cacheService = CacheService(stringTemplate)

        val input = "b-c"
        val resultA = "a-" + cacheService.hashString(input)

        assertThat(resultA).startsWith("a-")
    }
}