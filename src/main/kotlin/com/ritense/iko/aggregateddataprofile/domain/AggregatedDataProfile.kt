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
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "aggregated_data_profile",
    uniqueConstraints = [UniqueConstraint(columnNames = ["name", "version"])],
)
class AggregatedDataProfile(
    @Id
    val id: UUID,

    @Column(name = "name")
    var name: String,

    @Embedded
    val version: Version = Version("1.0.0"),

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false,

    @Column(name = "connector_instance_id")
    var connectorInstanceId: UUID,

    @Column(name = "connector_endpoint_id")
    var connectorEndpointId: UUID,

    @Embedded
    var endpointTransform: EndpointTransform,

    @OneToMany(
        cascade = [(CascadeType.ALL)],
        fetch = FetchType.EAGER,
        orphanRemoval = true,
        mappedBy = "aggregatedDataProfile",
    )
    var relations: MutableList<Relation> = mutableListOf(),

    @Embedded
    var resultTransform: Transform,

    @Embedded
    var roles: Roles,

    @Embedded
    var aggregatedDataProfileCacheSetting: AggregatedDataProfileCacheSetting,
) {

    fun handle(request: AggregatedDataProfileEditForm) {
        // Note: name and version is immutable after creation (not included in edit form)
        this.roles = Roles(request.roles)
        this.connectorEndpointId = request.connectorEndpointId
        this.connectorInstanceId = request.connectorInstanceId
        this.endpointTransform = EndpointTransform(request.endpointTransform)
        this.resultTransform = Transform(request.resultTransform)
        this.aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(
            enabled = request.cacheEnabled,
            timeToLive = request.cacheTimeToLive,
        )
    }

    fun addRelation(form: AddRelationForm) {
        this.relations.add(
            Relation(
                aggregatedDataProfile = this,
                sourceId = form.sourceId,
                resultTransform = Transform(form.resultTransform),
                endpointTransform = RelationEndpointTransform(form.sourceToEndpointMapping),
                connectorEndpointId = form.connectorEndpointId,
                connectorInstanceId = form.connectorInstanceId,
                propertyName = form.propertyName,
                relationCacheSettings = RelationCacheSettings(),
            ),
        )
    }

    fun changeRelation(form: EditRelationForm) {
        this.relations.removeIf { it.id == form.id }
        this.relations.add(
            Relation(
                id = form.id,
                aggregatedDataProfile = this,
                sourceId = form.sourceId,
                resultTransform = Transform(form.resultTransform),
                endpointTransform = RelationEndpointTransform(form.sourceToEndpointMapping),
                connectorInstanceId = form.connectorInstanceId,
                connectorEndpointId = form.connectorEndpointId,
                propertyName = form.propertyName,
                relationCacheSettings = RelationCacheSettings(
                    enabled = form.cacheEnabled,
                    timeToLive = form.cacheTimeToLive,
                ),
            ),
        )
    }

    fun removeRelation(request: DeleteRelationForm) {
        // Remove the selected relation and all its descendants
        val toRemove: MutableSet<UUID> = linkedSetOf()

        fun collectDescendants(parentId: UUID) {
            toRemove += parentId
            val children = this.relations.filter { it.sourceId == parentId }
            children.forEach { collectDescendants(it.id) }
        }
        collectDescendants(request.id)
        this.relations.removeIf { it.id in toRemove }
    }

    fun level1Relations(): List<Relation> {
        return relations.filter { it.sourceId == null || it.sourceId == this.id } // backwards compatible code either check on ADP id or null (used before prefilling)
    }

    fun relationsOf(id: UUID): List<Relation> = relations.filter {
        it.sourceId == id
    }

    /**
     * Creates a new version of this AggregatedDataProfile.
     * Relations are NOT copied here - they must be copied separately with ID remapping.
     */
    fun createNewVersion(newVersion: String): AggregatedDataProfile = AggregatedDataProfile(
        id = UUID.randomUUID(),
        name = this.name,
        version = Version(newVersion),
        isActive = false,
        connectorInstanceId = this.connectorInstanceId,
        connectorEndpointId = this.connectorEndpointId,
        endpointTransform = this.endpointTransform,
        resultTransform = this.resultTransform,
        roles = this.roles,
        aggregatedDataProfileCacheSetting = this.aggregatedDataProfileCacheSetting,
        relations = mutableListOf(),
    )

    companion object {
        fun create(form: AggregatedDataProfileAddForm): AggregatedDataProfile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = form.name,
            roles = Roles(form.roles),
            connectorInstanceId = form.connectorInstanceId,
            connectorEndpointId = form.connectorEndpointId,
            endpointTransform = EndpointTransform(form.endpointTransform),
            resultTransform = Transform(form.resultTransform),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
        )
    }
}