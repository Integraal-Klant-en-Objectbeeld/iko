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

package com.ritense.iko.mvc.model

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfileCacheSetting
import com.ritense.iko.aggregateddataprofile.domain.EndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.aggregateddataprofile.domain.RelationCacheSettings
import com.ritense.iko.aggregateddataprofile.domain.RelationEndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Roles
import com.ritense.iko.aggregateddataprofile.domain.Transform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class RelationTest {

    @Test
    fun `from maps relation entity to mvc model`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
            roles = Roles("ROLE_ADMIN"),
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

        val model = com.ritense.iko.mvc.model.Relation.from(relation)

        assertThat(model.aggregatedDataProfileId).isEqualTo(profile.id)
        assertThat(model.id).isEqualTo(relation.id)
        assertThat(model.sourceId).isEqualTo(profile.id)
        assertThat(model.connectorInstanceId).isEqualTo(relation.connectorInstanceId)
        assertThat(model.connectorEndpointId).isEqualTo(relation.connectorEndpointId)
        assertThat(model.propertyName).isEqualTo("owner")
        assertThat(model.sourceToEndpointMapping).isEqualTo("{\"id\": .source.ownerId}")
        assertThat(model.resultTransform).isEqualTo(".")
    }

    @Test
    fun `edit form can be created from relation`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
            roles = Roles("ROLE_ADMIN"),
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

        val form = EditRelationForm.from(relation)

        assertThat(form.aggregatedDataProfileId).isEqualTo(profile.id)
        assertThat(form.propertyName).isEqualTo("owner")
        assertThat(form.sourceToEndpointMapping).isEqualTo("{\"id\": .source.ownerId}")
        assertThat(form.resultTransform).isEqualTo(".")
        assertThat(form.connectorInstanceId).isEqualTo(relation.connectorInstanceId)
        assertThat(form.connectorEndpointId).isEqualTo(relation.connectorEndpointId)
    }
}