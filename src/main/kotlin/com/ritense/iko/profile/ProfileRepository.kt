package com.ritense.iko.profile

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfileRepository : JpaRepository<Profile, UUID> {
    fun findByName(name: String): Profile

    /** Case‑insensitive “contains” search, paged */
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable
    ): Page<Profile>

}