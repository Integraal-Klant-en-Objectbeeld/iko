package com.ritense.iko.cache.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal key/value caching service named RedisCacheService as requested.
 *
 * Note: This implementation does NOT depend on Spring Redis. It provides an
 * in-memory fallback with optional TTL per entry. If a real Redis integration
 * is added later, this class can be adapted to delegate to Redis operations
 * without changing its public API.
 */
@Service
class CacheService {

    private val log = KotlinLogging.logger {}

    private data class Entry(
        val value: String,
        val expiresAt: Long? // epoch millis; null means no TTL
    )

    private val store = ConcurrentHashMap<String, Entry>()

    /**
     * Store a value for the given key.
     *
     * @param key cache key
     * @param value cached value (stored as-is string)
     * @param ttl optional time-to-live; when null, the entry does not expire
     */
    fun put(key: String, value: String, ttl: Duration? = null) {
        val expiresAt = ttl?.let { System.currentTimeMillis() + it.toMillis() }
        store[key] = Entry(value, expiresAt)
        if (log.isDebugEnabled) {
            log.debug { "Cached key='$key' ttlMs='${ttl?.toMillis()}'" }
        }
    }

    /**
     * Retrieve a cached value by key, or null if missing or expired.
     */
    fun get(key: String): String? {
        val now = System.currentTimeMillis()
        val entry = store[key]
        if (entry == null) return null

        if (entry.expiresAt != null && entry.expiresAt <= now) {
            // Expired on access – evict it lazily
            store.remove(key)
            if (log.isDebugEnabled) {
                log.debug { "Cache miss (expired) for key='$key'" }
            }
            return null
        }
        return entry.value
    }

    /** Remove a specific key from the cache. */
    fun evict(key: String) {
        store.remove(key)
        if (log.isDebugEnabled) {
            log.debug { "Cache evicted key='$key'" }
        }
    }

    /** Clear all cached entries. */
    fun clear() {
        store.clear()
        if (log.isDebugEnabled) {
            log.debug { "Cache cleared" }
        }
    }

    fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

}