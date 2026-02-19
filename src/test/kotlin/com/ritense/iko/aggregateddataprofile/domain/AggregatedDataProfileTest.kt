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

import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.AggregatedDataProfileAddForm
import com.ritense.iko.mvc.model.AggregatedDataProfileEditForm
import com.ritense.iko.mvc.model.DeleteRelationForm
import com.ritense.iko.mvc.model.EditRelationForm
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class AggregatedDataProfileTest {

    @Test
    fun `create builds aggregated data profile from valid form`() {
        val form = AggregatedDataProfileAddForm(
            name = "pets",
            roles = "ROLE_ADMIN",
            endpointTransform = ".",
            resultTransform = ".",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
        )

        val profile = AggregatedDataProfile.create(form)

        assertThat(profile.name).isEqualTo("pets")
        assertThat(profile.roles.value).isEqualTo("ROLE_ADMIN")
        assertThat(profile.endpointTransform.expression).isEqualTo(".")
        assertThat(profile.resultTransform.expression).isEqualTo(".")
        assertThat(profile.connectorInstanceId).isEqualTo(form.connectorInstanceId)
        assertThat(profile.connectorEndpointId).isEqualTo(form.connectorEndpointId)
    }

    @Test
    fun `create throws when endpoint transform is invalid`() {
        val form = AggregatedDataProfileAddForm(
            name = "pets",
            roles = "ROLE_ADMIN",
            endpointTransform = "?",
            resultTransform = ".",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
        )

        assertThatThrownBy { AggregatedDataProfile.create(form) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid expression")
    }

    @Test
    fun `create throws when result transform is invalid`() {
        val form = AggregatedDataProfileAddForm(
            name = "pets",
            roles = "ROLE_ADMIN",
            endpointTransform = ".",
            resultTransform = "?",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
        )

        assertThatThrownBy { AggregatedDataProfile.create(form) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid expression")
    }

    @Test
    fun `create throws when roles is invalid`() {
        val form = AggregatedDataProfileAddForm(
            name = "pets",
            roles = "ROLE ADMIN",
            endpointTransform = ".",
            resultTransform = ".",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
        )

        assertThatThrownBy { AggregatedDataProfile.create(form) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Roles must be a comma-separated list of values")
    }

    @Test
    fun `handle updates profile from edit form but preserves name`() {
        val profile = createProfile()
        val originalName = profile.name
        val originalVersion = profile.version
        val newConnectorInstanceId = UUID.randomUUID()
        val newConnectorEndpointId = UUID.randomUUID()
        val form = AggregatedDataProfileEditForm(
            id = profile.id,
            name = "updated-name",
            roles = "ROLE_UPDATED",
            connectorInstanceId = newConnectorInstanceId,
            connectorEndpointId = newConnectorEndpointId,
            endpointTransform = ".updated",
            resultTransform = ".result",
            cacheEnabled = true,
            cacheTimeToLive = 3600,
            version = Version("2.0.0"),
        )

        profile.handle(form)

        // Name and version should remain unchanged (immutable after creation)
        assertThat(profile.name).isEqualTo(originalName)
        assertThat(profile.version).isEqualTo(originalVersion)
        assertThat(profile.roles.value).isEqualTo("ROLE_UPDATED")
        assertThat(profile.connectorInstanceId).isEqualTo(newConnectorInstanceId)
        assertThat(profile.connectorEndpointId).isEqualTo(newConnectorEndpointId)
        assertThat(profile.endpointTransform.expression).isEqualTo(".updated")
        assertThat(profile.resultTransform.expression).isEqualTo(".result")
        assertThat(profile.aggregatedDataProfileCacheSetting.enabled).isTrue
        assertThat(profile.aggregatedDataProfileCacheSetting.timeToLive).isEqualTo(3600)
    }

    @Test
    fun `addRelation appends a relation with default cache settings`() {
        val profile = createProfile()
        val form = AddRelationForm(
            aggregatedDataProfileId = profile.id,
            sourceId = profile.id,
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            sourceToEndpointMapping = "{\"id\": .source.ownerId}",
            resultTransform = ".",
            propertyName = "owner",
        )

        profile.addRelation(form)

        assertThat(profile.relations).hasSize(1)
        val relation = profile.relations.first()
        assertThat(relation.aggregatedDataProfile).isSameAs(profile)
        assertThat(relation.sourceId).isEqualTo(form.sourceId)
        assertThat(relation.propertyName).isEqualTo(form.propertyName)
        assertThat(relation.connectorInstanceId).isEqualTo(form.connectorInstanceId)
        assertThat(relation.connectorEndpointId).isEqualTo(form.connectorEndpointId)
        assertThat(relation.endpointTransform.expression).isEqualTo(form.sourceToEndpointMapping)
        assertThat(relation.resultTransform.expression).isEqualTo(form.resultTransform)
        assertThat(relation.relationCacheSettings.enabled).isFalse
        assertThat(relation.relationCacheSettings.timeToLive).isEqualTo(0)
    }

    @Test
    fun `changeRelation replaces existing relation and updates cache settings`() {
        val profile = createProfile()
        val relationId = UUID.randomUUID()
        val existing = Relation(
            id = relationId,
            aggregatedDataProfile = profile,
            propertyName = "owner",
            sourceId = profile.id,
            endpointTransform = RelationEndpointTransform("{\"id\": .source.ownerId}"),
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            resultTransform = Transform("."),
            relationCacheSettings = RelationCacheSettings(),
        )
        profile.relations.add(existing)
        val form = EditRelationForm(
            aggregatedDataProfileId = profile.id,
            id = relationId,
            sourceId = profile.id,
            sourceToEndpointMapping = "{\"id\": .source.customerId}",
            resultTransform = ".",
            propertyName = "customer",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            cacheEnabled = true,
            cacheTimeToLive = 60000,
        )

        profile.changeRelation(form)

        assertThat(profile.relations).hasSize(1)
        val updated = profile.relations.first()
        assertThat(updated).isNotSameAs(existing)
        assertThat(updated.id).isEqualTo(relationId)
        assertThat(updated.propertyName).isEqualTo(form.propertyName)
        assertThat(updated.connectorInstanceId).isEqualTo(form.connectorInstanceId)
        assertThat(updated.connectorEndpointId).isEqualTo(form.connectorEndpointId)
        assertThat(updated.endpointTransform.expression).isEqualTo(form.sourceToEndpointMapping)
        assertThat(updated.resultTransform.expression).isEqualTo(form.resultTransform)
        assertThat(updated.relationCacheSettings.enabled).isTrue
        assertThat(updated.relationCacheSettings.timeToLive).isEqualTo(form.cacheTimeToLive)
    }

    @Test
    fun `removeRelation removes relation and descendants`() {
        val profile = createProfile()
        val rootId = UUID.randomUUID()
        val childId = UUID.randomUUID()
        val grandChildId = UUID.randomUUID()
        val otherId = UUID.randomUUID()
        profile.relations.addAll(
            listOf(
                Relation(
                    id = rootId,
                    aggregatedDataProfile = profile,
                    propertyName = "root",
                    sourceId = profile.id,
                    endpointTransform = RelationEndpointTransform("{\"id\": .source.rootId}"),
                    connectorInstanceId = UUID.randomUUID(),
                    connectorEndpointId = UUID.randomUUID(),
                    resultTransform = Transform("."),
                    relationCacheSettings = RelationCacheSettings(),
                ),
                Relation(
                    id = childId,
                    aggregatedDataProfile = profile,
                    propertyName = "child",
                    sourceId = rootId,
                    endpointTransform = RelationEndpointTransform("{\"id\": .source.childId}"),
                    connectorInstanceId = UUID.randomUUID(),
                    connectorEndpointId = UUID.randomUUID(),
                    resultTransform = Transform("."),
                    relationCacheSettings = RelationCacheSettings(),
                ),
                Relation(
                    id = grandChildId,
                    aggregatedDataProfile = profile,
                    propertyName = "grand",
                    sourceId = childId,
                    endpointTransform = RelationEndpointTransform("{\"id\": .source.grandId}"),
                    connectorInstanceId = UUID.randomUUID(),
                    connectorEndpointId = UUID.randomUUID(),
                    resultTransform = Transform("."),
                    relationCacheSettings = RelationCacheSettings(),
                ),
                Relation(
                    id = otherId,
                    aggregatedDataProfile = profile,
                    propertyName = "other",
                    sourceId = profile.id,
                    endpointTransform = RelationEndpointTransform("{\"id\": .source.otherId}"),
                    connectorInstanceId = UUID.randomUUID(),
                    connectorEndpointId = UUID.randomUUID(),
                    resultTransform = Transform("."),
                    relationCacheSettings = RelationCacheSettings(),
                ),
            ),
        )

        profile.removeRelation(DeleteRelationForm(profile.id, rootId))

        assertThat(profile.relations.map { it.id }).containsOnly(otherId)
    }

    @Test
    fun `level1Relations returns relations at profile level`() {
        val profile = createProfile()
        val level1Id = UUID.randomUUID()
        val nullSourceId = UUID.randomUUID()
        val otherParentId = UUID.randomUUID()
        profile.relations.addAll(
            listOf(
                Relation(
                    id = level1Id,
                    aggregatedDataProfile = profile,
                    propertyName = "level1",
                    sourceId = profile.id,
                    endpointTransform = RelationEndpointTransform("{\"id\": .source.level1}"),
                    connectorInstanceId = UUID.randomUUID(),
                    connectorEndpointId = UUID.randomUUID(),
                    resultTransform = Transform("."),
                    relationCacheSettings = RelationCacheSettings(),
                ),
                Relation(
                    id = nullSourceId,
                    aggregatedDataProfile = profile,
                    propertyName = "nullSource",
                    sourceId = null,
                    endpointTransform = RelationEndpointTransform("{\"id\": .source.nullSource}"),
                    connectorInstanceId = UUID.randomUUID(),
                    connectorEndpointId = UUID.randomUUID(),
                    resultTransform = Transform("."),
                    relationCacheSettings = RelationCacheSettings(),
                ),
                Relation(
                    id = UUID.randomUUID(),
                    aggregatedDataProfile = profile,
                    propertyName = "child",
                    sourceId = otherParentId,
                    endpointTransform = RelationEndpointTransform("{\"id\": .source.child}"),
                    connectorInstanceId = UUID.randomUUID(),
                    connectorEndpointId = UUID.randomUUID(),
                    resultTransform = Transform("."),
                    relationCacheSettings = RelationCacheSettings(),
                ),
            ),
        )

        assertThat(profile.level1Relations().map { it.id }).containsOnly(level1Id, nullSourceId)
    }

    @Test
    fun `create returns profile with isActive true`() {
        val form = AggregatedDataProfileAddForm(
            name = "pets",
            roles = "ROLE_ADMIN",
            endpointTransform = ".",
            resultTransform = ".",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
        )

        val profile = AggregatedDataProfile.create(form)

        assertThat(profile.isActive).isTrue()
    }

    @Test
    fun `createNewVersion returns profile with isActive false`() {
        val profile = createProfile()
        profile.isActive = true

        val newVersion = profile.createNewVersion("2.0.0")

        assertThat(newVersion.isActive).isFalse()
        assertThat(newVersion.version.value).isEqualTo("2.0.0")
        assertThat(newVersion.name).isEqualTo(profile.name)
        assertThat(newVersion.id).isNotEqualTo(profile.id)
        assertThat(newVersion.relations).isEmpty()
    }

    @Test
    fun `relationsOf returns relations that match source id`() {
        val profile = createProfile()
        val targetId = UUID.randomUUID()
        val matchId = UUID.randomUUID()
        profile.relations.addAll(
            listOf(
                Relation(
                    id = matchId,
                    aggregatedDataProfile = profile,
                    propertyName = "match",
                    sourceId = targetId,
                    endpointTransform = RelationEndpointTransform("{\"id\": .source.match}"),
                    connectorInstanceId = UUID.randomUUID(),
                    connectorEndpointId = UUID.randomUUID(),
                    resultTransform = Transform("."),
                    relationCacheSettings = RelationCacheSettings(),
                ),
                Relation(
                    aggregatedDataProfile = profile,
                    propertyName = "other",
                    sourceId = UUID.randomUUID(),
                    endpointTransform = RelationEndpointTransform("{\"id\": .source.other}"),
                    connectorInstanceId = UUID.randomUUID(),
                    connectorEndpointId = UUID.randomUUID(),
                    resultTransform = Transform("."),
                    relationCacheSettings = RelationCacheSettings(),
                ),
            ),
        )

        assertThat(profile.relationsOf(targetId).map { it.id }).containsOnly(matchId)
    }

    private fun createProfile(): AggregatedDataProfile = AggregatedDataProfile(
        id = UUID.randomUUID(),
        name = "pets",
        connectorInstanceId = UUID.randomUUID(),
        connectorEndpointId = UUID.randomUUID(),
        endpointTransform = EndpointTransform("."),
        resultTransform = Transform("."),
        roles = Roles("ROLE_TEST"),
        aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
        version = Version("1.0.0"),
    )
}