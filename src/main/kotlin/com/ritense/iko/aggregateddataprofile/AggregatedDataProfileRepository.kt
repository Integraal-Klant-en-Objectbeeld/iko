package com.ritense.iko.aggregateddataprofile

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

    /** Case‑insensitive “contains” search, paged */
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable
    ): Page<AggregatedDataProfile>

    fun existsByName(name: String): Boolean

    @Query(
        """
        SELECT  adp.id
        ,       adp.name
        ,       adp.name as primaryEndpoint
        FROM    aggregated_data_profile adp
        JOIN    endpoint e ON adp.primary_endpoint = e.id
        """,
        countQuery = """
        SELECT  count(*)
        FROM    aggregated_data_profile adp
        JOIN    endpoint e ON adp.primary_endpoint = e.id
        """,
        nativeQuery = true
    )
    fun findAllBy(pageable: Pageable): Page<AggregatedDataProfileListItem>

    @Query(
        """
        SELECT  adp.id
        ,       adp.name
        ,       e.name as primaryEndpoint
        FROM    aggregated_data_profile adp
        JOIN    endpoint e
        ON      adp.primary_endpoint = e.id
        WHERE   LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """,
        countQuery = """
        SELECT  COUNT(*)
        FROM    aggregated_data_profile adp
        JOIN    endpoint e
        ON      adp.primary_endpoint = e.id
        WHERE   LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """,
        nativeQuery = true
    )
    fun findAllByName(@Param("name") name: String, pageable: Pageable): Page<AggregatedDataProfileListItem>

    interface AggregatedDataProfileListItem {
        val id: String
        val name: String
        val primaryEndpoint: String
    }
}