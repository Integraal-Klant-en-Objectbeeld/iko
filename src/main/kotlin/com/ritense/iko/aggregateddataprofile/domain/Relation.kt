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

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.util.UUID

@Entity
class Relation(
    @Id
    val id: UUID = UUID.randomUUID(),
    @JoinColumn(name = "aggregated_data_profile_id")
    @ManyToOne
    var aggregatedDataProfile: AggregatedDataProfile,
    @Column(name = "property_name")
    var propertyName: String,
    @Column(name = "source_id")
    var sourceId: UUID? = null,
    @Embedded
    var endpointTransform: RelationEndpointTransform,
    @Column(name = "connector_instance_id")
    var connectorInstanceId: UUID,
    @Column(name = "connector_endpoint_id")
    var connectorEndpointId: UUID,
    @Embedded
    var resultTransform: Transform,
    @Embedded
    var relationCacheSettings: RelationCacheSettings,
) {
    /**
     * Creates a copy of this relation for a new ADP version.
     * Note: sourceId is set to null and must be remapped after all relations are copied.
     */
    fun copyForNewVersion(newAdp: AggregatedDataProfile): Relation = Relation(
        id = UUID.randomUUID(),
        aggregatedDataProfile = newAdp,
        propertyName = this.propertyName,
        sourceId = null, // Will be remapped after all relations are copied
        endpointTransform = this.endpointTransform,
        connectorInstanceId = this.connectorInstanceId,
        connectorEndpointId = this.connectorEndpointId,
        resultTransform = this.resultTransform,
        relationCacheSettings = this.relationCacheSettings,
    )
}