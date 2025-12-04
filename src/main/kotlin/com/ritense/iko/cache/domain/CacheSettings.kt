package com.ritense.iko.cache.domain

interface CacheSettings {
    val enabled: Boolean
    val timeToLive: Int
}