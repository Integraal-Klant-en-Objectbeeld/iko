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

package com.ritense.iko.connectors.domain

import com.ritense.iko.crypto.AesGcmStringAttributeConverter
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "connector_instance")
class ConnectorInstance(
    @Id
    var id: UUID,
    @Column(name = "name")
    var name: String,
    @ManyToOne
    @JoinColumn(name = "connector_id")
    var connector: Connector,
    @Column(name = "tag")
    var tag: String,
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @Convert(attributeName = "value", converter = AesGcmStringAttributeConverter::class)
    @CollectionTable(
        name = "connector_instance_config",
        joinColumns = [JoinColumn(name = "connector_instance_id")],
    )
    var config: Map<String, String>,
) {
    /**
     * Creates a copy of this instance for a new Connector version.
     * Note: connector must be set by the caller after creation.
     */
    fun copyForNewConnector(newConnector: Connector): ConnectorInstance = ConnectorInstance(
        id = UUID.randomUUID(),
        name = this.name,
        connector = newConnector,
        tag = this.tag,
        config = this.config.toMap(), // Create a copy of the config map
    )
}