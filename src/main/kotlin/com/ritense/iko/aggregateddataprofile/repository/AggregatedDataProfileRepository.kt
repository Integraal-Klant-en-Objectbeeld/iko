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

package com.ritense.iko.aggregateddataprofile.repository

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AggregatedDataProfileRepository : JpaRepository<AggregatedDataProfile, UUID> {
    fun findByName(name: String): AggregatedDataProfile?

    // Version-aware queries
    fun findByNameAndIsActiveTrue(name: String): AggregatedDataProfile?

    @Query("SELECT a FROM AggregatedDataProfile a WHERE a.name = :name AND a.version.value = :version")
    fun findByNameAndVersion(
        @Param("name") name: String,
        @Param("version") version: String,
    ): AggregatedDataProfile?

    fun findAllByNameOrderByVersionDesc(name: String): List<AggregatedDataProfile>

    fun findAllByIsActiveTrue(): List<AggregatedDataProfile>

    @Query(
        """
        SELECT  adp.id
        ,       adp.name
        FROM    aggregated_data_profile adp
        """,
        countQuery = """
        SELECT  count(*)
        FROM    aggregated_data_profile adp
        """,
        nativeQuery = true,
    )
    fun findAllBy(pageable: Pageable): Page<AggregatedDataProfileListItem>

    @Query(
        """
        SELECT  adp.id
        ,       adp.name
        FROM    aggregated_data_profile adp
        WHERE   LOWER(adp.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """,
        countQuery = """
        SELECT  COUNT(*)
        FROM    aggregated_data_profile adp
        WHERE   LOWER(adp.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """,
        nativeQuery = true,
    )
    fun findAllByName(
        @Param("name") name: String,
        pageable: Pageable,
    ): Page<AggregatedDataProfileListItem>

    @Query(
        """
        SELECT  adp.id as id
        ,       adp.name as name
        ,       adp.version.value as version
        ,       adp.isActive as isActive
        FROM    AggregatedDataProfile adp
        WHERE   adp.name = :name
        ORDER BY adp.version.value DESC
        """,
    )
    fun findVersionsByName(@Param("name") name: String): List<AggregatedDataProfileVersionProjection>

    interface AggregatedDataProfileListItem {
        val id: String
        val name: String
    }

    interface AggregatedDataProfileVersionProjection {
        val id: UUID
        val name: String
        val version: String
        val isActive: Boolean
    }
}