package com.ritense.iko.search

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SearchRepository : JpaRepository<Search, UUID> {
    fun findByName(name: String): Search
    fun findAllByIsPrimaryTrueOrderByName(): List<Search>
}