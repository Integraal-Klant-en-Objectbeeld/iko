package com.ritense.iko.profile

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProfileRepository : JpaRepository<Profile, UUID> {
    fun findByName(name: String): Profile

    /** Case‑insensitive “contains” search, paged */
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable
    ): Page<Profile>

    fun existsByName(name: String): Boolean

    @Query(
        """
        SELECT  p.id
        ,       p.name
        ,       s.name as primarySearch
        FROM    profile p
        JOIN    search s ON p.primary_search = s.id
        """,
        countQuery = """
        SELECT  count(*)
        FROM    profile p
        JOIN    search s ON p.primary_search = s.id
        """,
        nativeQuery = true
    )
    fun findAllBy(pageable: Pageable): Page<ProfileListItem>

    interface ProfileListItem {
        val id: String
        val name: String
        val primarySearch: String
    }

    @Query(
        """
        SELECT  p.id,
                p.name,
                s.name as primarySearch
        FROM    profile p
        JOIN    search s ON p.primary_search = s.id
        WHERE   LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """,
        countQuery = """
        SELECT  COUNT(*)
        FROM    profile p
        JOIN    search s ON p.primary_search = s.id
        WHERE   LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """,
        nativeQuery = true
    )
    fun findAllByName(@Param("name") name: String, pageable: Pageable): Page<ProfileListItem>

}