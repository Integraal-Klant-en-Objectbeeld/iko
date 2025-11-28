package com.ritense.iko.cache.domain

import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.HIT
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.PUT
import java.time.Duration

data class CacheEntry(
    val type: CacheEventType,
    val key: String,
    val value: String? = null,
    val timeToLive: Duration? = null,
) {
    init {
        if (type == PUT) requireNotNull(value)
        if (type == HIT) requireNotNull(value)
    }

    enum class CacheEventType {
        PUT,
        HIT,
        MISS
    }
}
