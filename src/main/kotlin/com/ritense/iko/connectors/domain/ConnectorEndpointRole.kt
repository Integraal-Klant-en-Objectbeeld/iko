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

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "connector_endpoint_role")
class ConnectorEndpointRole(
    @Id
    var id: UUID,
    @ManyToOne
    @JoinColumn(name = "connector_endpoint_id")
    var connectorEndpoint: ConnectorEndpoint,
    @ManyToOne
    @JoinColumn(name = "connector_instance_id")
    var connectorInstance: ConnectorInstance,
    @Column(name = "role")
    var role: String,
) {
    companion object {
        fun create(
            connectorInstance: ConnectorInstance,
            connectorEndpoint: ConnectorEndpoint,
            role: String,
        ): ConnectorEndpointRole = ConnectorEndpointRole(
            id = UUID.randomUUID(),
            connectorInstance = connectorInstance,
            connectorEndpoint = connectorEndpoint,
            role = role,
        )
    }
}