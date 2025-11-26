package com.ritense.iko.aggregateddataprofile.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class AggregatedDataProfileCacheSetting(
    @Column(name = "cache_enabled")
    val enabled: Boolean = false,
    @Column(name = "cache_ttl")
    // Time to live for cache entries (in ms)
    val timeToLive: Int = 0
)