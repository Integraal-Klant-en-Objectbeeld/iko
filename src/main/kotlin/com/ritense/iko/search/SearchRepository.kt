package com.ritense.iko.search

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SearchRepository : JpaRepository<Search, UUID> {
    fun findByName(name: String): Search
    fun findAllByIsPrimaryTrueOrderByName(): List<Search>

    /** Case‑insensitive “contains” search, paged */
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable
    ): Page<Search>
}