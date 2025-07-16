package com.ritense.iko.endpoints

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EndpointRepository : JpaRepository<Endpoint, UUID> {
    fun findByName(name: String): Endpoint

    fun findAllByIsPrimaryTrueOrderByName(): List<Endpoint>

    /** Case‑insensitive “contains” search, paged */
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable
    ): Page<Endpoint>
}