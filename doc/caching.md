# Caching

IKO uses Redis as a caching layer to reduce redundant calls to external systems. Caching is configurable per profile and per relation.

## How It Works

The cache is integrated into the Camel route execution pipeline via `CacheProcessor`. When a profile or relation endpoint is about to be called:

1. A cache key is computed from the profile/relation ID, JQ expressions, and request parameters.
2. Redis is checked for an existing entry.
3. **Cache HIT**: The cached value is returned directly, skipping the external call.
4. **Cache MISS**: The external endpoint is called, and the result is stored in Redis with the configured TTL.

## Configuration

Cache settings are embedded in the domain entities:

### Profile-level cache

Configured on `AggregatedDataProfile.aggregatedDataProfileCacheSetting`:

| Field | Type | Description |
|---|---|---|
| `enabled` | Boolean | Whether caching is active for the primary endpoint |
| `timeToLive` | Int | TTL in milliseconds |

### Relation-level cache

Configured on `Relation.relationCacheSettings`:

| Field | Type | Description |
|---|---|---|
| `enabled` | Boolean | Whether caching is active for this relation's endpoint |
| `timeToLive` | Int | TTL in milliseconds |

Cache settings are managed through the admin UI when editing a profile or relation.

## Cache Eviction

Cache entries can be manually evicted through the admin UI or API:

- **Profile cache**: `DELETE /admin/aggregated-data-profiles/{id}/cache` — evicts all cached entries for the profile.
- **Relation cache**: `DELETE /admin/aggregated-data-profiles/{id}/relation/{relationId}/cache` — evicts cached entries for a specific relation.

Eviction works by prefix matching on the cache key, so evicting a profile's cache also clears all related entries.

## Cache Key Structure

Cache keys are computed from:
- Profile or relation ID
- JQ endpoint transform expression
- JQ result transform expression
- Actual endpoint result data

This ensures that different query parameters produce different cache entries.

## Infrastructure

- **Redis client**: Jedis (via `spring-boot-starter-data-redis`)
- **Service**: `CacheService` wraps Spring Data Redis operations
- **Camel integration**: `CacheProcessor` acts as a Camel processor in the route pipeline
- **Configuration**: Redis host and port are set via environment variables (see `.env.template`)