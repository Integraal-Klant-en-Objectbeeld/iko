/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
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

package com.ritense.iko.aggregateddataprofile.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class RelationTest {

    @Test
    fun `constructs relation with valid transforms`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            roles = Roles("ROLE_TEST"),
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
            relationCacheSettings = RelationCacheSettings(),
        )

        assertThat(relation.propertyName).isEqualTo("owner")
        assertThat(relation.endpointTransform.expression).isEqualTo("{\"id\": .source.ownerId}")
        assertThat(relation.resultTransform.expression).isEqualTo(".")
    }

    @Test
    fun `constructing relation with invalid endpoint transform throws`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            roles = Roles("ROLE_TEST"),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
        )

        assertThatThrownBy {
            Relation(
                aggregatedDataProfile = profile,
                propertyName = "owner",
                sourceId = profile.id,
                endpointTransform = RelationEndpointTransform("?"),
                connectorInstanceId = UUID.randomUUID(),
                connectorEndpointId = UUID.randomUUID(),
                resultTransform = Transform("."),
                relationCacheSettings = RelationCacheSettings(),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid expression")
    }

    @Test
    fun `constructing relation with invalid result transform throws`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            roles = Roles("ROLE_TEST"),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
        )

        assertThatThrownBy {
            Relation(
                aggregatedDataProfile = profile,
                propertyName = "owner",
                sourceId = profile.id,
                endpointTransform = RelationEndpointTransform("{\"id\": .source.ownerId}"),
                connectorInstanceId = UUID.randomUUID(),
                connectorEndpointId = UUID.randomUUID(),
                resultTransform = Transform("?"),
                relationCacheSettings = RelationCacheSettings(),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid expression")
    }
}