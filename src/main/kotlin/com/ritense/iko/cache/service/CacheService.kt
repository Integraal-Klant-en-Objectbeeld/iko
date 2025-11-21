package com.ritense.iko.cache.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import java.security.MessageDigest
import java.time.Duration

/**
 * Small Redis-backed key/value caching service used by the application routes.
 *
 * What it does
 * - Stores and retrieves String values by key using Spring's [StringRedisTemplate].
 * - Supports an optional time-to-live (TTL) per entry; when omitted the entry will not expire.
 * - Provides a utility to create deterministic SHA-256 hashes for constructing cache keys.
 *
 * Notes
 * - All values are stored as-is Strings. If you need objects, serialize/deserialize in the caller.
 * - This service is state-less and thread-safe; it delegates concurrency to Redis.
 * - Evict removes only the provided key; it does not support patterns/wildcards.
 */
class CacheService(
    private val template: StringRedisTemplate
) {

    /**
     * Store a value for the given key.
     *
     * @param key Cache key. Callers typically use [hashString] to generate a stable key.
     * @param value Cached value (stored as-is String).
     * @param ttl Optional time-to-live; when null, the entry does not expire.
     */
    fun put(key: String, value: String, ttl: Duration? = null) {
        val ops = template.opsForValue()
        if (ttl == null) {
            ops.set(key, value)
        } else {
            ops.set(key, value, ttl)
        }
        logger.debug { "Cached key='$key' ttlMs='${ttl?.toMillis()}'" }
    }

    /**
     * Retrieve a cached value by key.
     *
     * @param key Cache key.
     * @return The cached String value, or null when the key is absent or expired.
     */
    fun get(key: String): String? {
        return template.opsForValue().get(key)
    }

    /**
     * Remove a specific key from the cache.
     *
     * @param key Cache key to remove.
     */
    fun evict(key: String) {
        template.delete(key)
        logger.debug { "Cache evicted key='$key'" }
    }

    /**
     * Create a deterministic, lower-case hex-encoded SHA-256 hash for the given input.
     *
     * This is convenient for turning long or sensitive inputs (e.g., URLs with query params)
     * into compact cache keys without leaking the original data.
     *
     * @param input Any String to hash (UTF-8 bytes are used).
     * @return 64-character hex String of the SHA-256 digest.
     */
    fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}