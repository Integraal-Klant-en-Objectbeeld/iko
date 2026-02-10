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
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "connector",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tag", "version"])],
)
class Connector(
    @Id
    var id: UUID,
    @Column(name = "name")
    var name: String,
    @Column(name = "tag")
    var tag: String,
    @Embedded
    val version: Version = Version("1.0.0"),
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false,
    @Column(name = "connector_code")
    var connectorCode: String,
) {
    /**
     * Creates a new version of this Connector.
     * Endpoints and instances are NOT copied here - they must be copied separately.
     */
    fun createNewVersion(newVersion: String): Connector = Connector(
        id = UUID.randomUUID(),
        name = this.name,
        tag = this.tag,
        version = Version(newVersion),
        isActive = false,
        connectorCode = this.connectorCode,
    )
}