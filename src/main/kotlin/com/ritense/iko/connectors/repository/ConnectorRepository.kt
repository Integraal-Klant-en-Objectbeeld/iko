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

package com.ritense.iko.connectors.repository

import com.ritense.iko.connectors.domain.Connector
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ConnectorRepository : JpaRepository<Connector, UUID> {
    // Version-aware queries
    fun findByTagAndIsActiveTrue(tag: String): Connector?

    @Query("SELECT c FROM Connector c WHERE c.tag = :tag AND c.version.value = :version")
    fun findByTagAndVersion(
        @Param("tag") tag: String,
        @Param("version") version: String,
    ): Connector?

    fun findAllByTagOrderByVersionDesc(tag: String): List<Connector>

    fun findAllByIsActiveTrue(): List<Connector>

    @Query(
        """
        SELECT  c.id as id
        ,       c.name as name
        ,       c.tag as tag
        ,       c.version.value as version
        ,       c.isActive as isActive
        FROM    Connector c
        WHERE   c.tag = :tag
        ORDER BY c.version.value DESC
        """,
    )
    fun findVersionsByTag(@Param("tag") tag: String): List<ConnectorVersionProjection>

    interface ConnectorVersionProjection {
        val id: UUID
        val name: String
        val tag: String
        val version: String
        val isActive: Boolean
    }
}