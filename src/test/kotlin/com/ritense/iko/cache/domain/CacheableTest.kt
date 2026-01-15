package com.ritense.iko.cache.domain

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfileCacheSetting
import com.ritense.iko.aggregateddataprofile.domain.EndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.ENDPOINT_TRANSFORM_RESULT_VARIABLE
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.aggregateddataprofile.domain.RelationCacheSettings
import com.ritense.iko.aggregateddataprofile.domain.RelationEndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Transform
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class CacheableTest {
    private val context = DefaultCamelContext()

    @Test
    fun `adp toCacheable builds cache key and handles hit`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(
                enabled = true,
                timeToLive = 250,
            ),
            role = "ROLE_ADMIN",
        )

        val exchange = DefaultExchange(context)
        exchange.setVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, "mapping")

        val cacheable = profile.toCacheable()
        val key = cacheable.cacheKey(exchange)

        assertThat(key).contains(profile.id.toString())
        assertThat(key).contains(profile.endpointTransform.expression)
        assertThat(key).contains("mapping")
        assertThat(key).contains(profile.resultTransform.expression)
        assertThat(cacheable.cacheSettings.enabled).isTrue()
        assertThat(cacheable.cacheSettings.timeToLive).isEqualTo(250)

        cacheable.handleCacheEntry(
            exchange,
            CacheEntry(type = CacheEntry.CacheEventType.HIT, key = "key", value = """{"ok":true}"""),
        )

        assertThat(exchange.message.getBody(String::class.java)).isEqualTo("""{"ok":true}""")
        assertThat(exchange.message.getHeader(Exchange.CONTENT_TYPE)).isEqualTo("application/json")
        assertThat(exchange.isRouteStop).isTrue()
    }

    @Test
    fun `relation toCacheable builds cache key and handles hit and miss`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
        )
        val relation = Relation(
            aggregatedDataProfile = profile,
            propertyName = "owner",
            sourceId = profile.id,
            endpointTransform = RelationEndpointTransform("{\"id\": .source.ownerId}"),
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            resultTransform = Transform("."),
            relationCacheSettings = RelationCacheSettings(
                enabled = true,
                timeToLive = 500,
            ),
        )

        val exchange = DefaultExchange(context)
        exchange.setVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, "relationMapping")

        val cacheable = relation.toCacheable()
        val key = cacheable.cacheKey(exchange)

        assertThat(key).contains(relation.id.toString())
        assertThat(key).contains(relation.endpointTransform.toString())
        assertThat(key).contains("relationMapping")
        assertThat(key).contains(relation.resultTransform.expression)
        assertThat(cacheable.cacheSettings.enabled).isTrue()
        assertThat(cacheable.cacheSettings.timeToLive).isEqualTo(500)

        cacheable.handleCacheEntry(
            exchange,
            CacheEntry(type = CacheEntry.CacheEventType.HIT, key = "key", value = """{"ok":true}"""),
        )

        assertThat(exchange.message.getBody(String::class.java)).isEqualTo("""{"ok":true}""")
        assertThat(exchange.message.getHeader(Exchange.CONTENT_TYPE)).isEqualTo("application/json")
        assertThat(exchange.getVariable("cacheHit_${relation.id}", Boolean::class.java)).isTrue()

        cacheable.handleCacheEntry(
            exchange,
            CacheEntry(type = CacheEntry.CacheEventType.MISS, key = "key"),
        )

        assertThat(exchange.getVariable("cacheHit_${relation.id}", Boolean::class.java)).isFalse()
    }
}