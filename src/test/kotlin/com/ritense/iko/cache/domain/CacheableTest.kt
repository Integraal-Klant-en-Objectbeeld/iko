/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.cache.domain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfileCacheSetting
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfileSchema
import com.ritense.iko.aggregateddataprofile.domain.EndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.aggregateddataprofile.domain.RelationCacheSettings
import com.ritense.iko.aggregateddataprofile.domain.RelationEndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Roles
import com.ritense.iko.aggregateddataprofile.domain.Transform
import com.ritense.iko.camel.IkoConstants.Variables.ENDPOINT_TRANSFORM_CONTEXT_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.ENDPOINT_TRANSFORM_RESULT_VARIABLE
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class CacheableTest {
    private val context = DefaultCamelContext()
    private val objectMapper = jacksonObjectMapper()

    private fun transformContext(idParam: String) = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(
        mapOf(
            "idParam" to idParam,
            "sortParams" to emptyMap<String, Any>(),
            "filterParams" to emptyMap<String, Any>(),
        ),
    )

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
            roles = Roles("ROLE_ADMIN"),
            schema = null,
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
            roles = Roles("ROLE_TEST"),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
            schema = null,
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
        assertThat(key).contains(relation.endpointTransform.expression)
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

    @Test
    fun `adp cacheKey differs when only idParam differs`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            // deliberately omits .idParam, so the JQ output is identical for both residents
            endpointTransform = EndpointTransform("{}"),
            resultTransform = Transform("."),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(
                enabled = true,
                timeToLive = 250,
            ),
            roles = Roles("ROLE_ADMIN"),
            schema = null,
        )
        val cacheable = profile.toCacheable()

        val exchangeA = DefaultExchange(context)
        exchangeA.setVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, transformContext("A"))
        exchangeA.setVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, "mapping")

        val exchangeB = DefaultExchange(context)
        exchangeB.setVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, transformContext("B"))
        exchangeB.setVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, "mapping")

        val keyA = cacheable.cacheKey(exchangeA)
        val keyB = cacheable.cacheKey(exchangeB)

        assertThat(keyA).isNotEqualTo(keyB)
        assertThat(keyA).contains("A")
        assertThat(keyB).contains("B")
    }

    @Test
    fun `relation cacheKey differs when only idParam differs and contains the relation JQ expression`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            roles = Roles("ROLE_TEST"),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
            schema = null,
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
        val cacheable = relation.toCacheable()

        val exchangeA = DefaultExchange(context)
        exchangeA.setVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, transformContext("A"))
        exchangeA.setVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, "relationMapping")

        val exchangeB = DefaultExchange(context)
        exchangeB.setVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, transformContext("B"))
        exchangeB.setVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, "relationMapping")

        val keyA = cacheable.cacheKey(exchangeA)
        val keyB = cacheable.cacheKey(exchangeB)

        assertThat(keyA).isNotEqualTo(keyB)
        assertThat(keyA).contains(relation.endpointTransform.expression)
        assertThat(keyB).contains(relation.endpointTransform.expression)
    }
}