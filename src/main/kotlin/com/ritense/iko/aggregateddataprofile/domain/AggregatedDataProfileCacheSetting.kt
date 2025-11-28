package com.ritense.iko.aggregateddataprofile.domain

import com.ritense.iko.cache.domain.CacheSettings
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class AggregatedDataProfileCacheSetting(
    @Column(name = "cache_enabled")
    override val enabled: Boolean = false,
    @Column(name = "cache_ttl")
    // Time to live for cache entries (in ms)
    override val timeToLive: Int = 0
) : CacheSettings