package com.ritense.iko.aggregateddataprofile.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Roles(
    @Column(name = "roles", nullable = false)
    val value: String,
) {
    fun asList(): List<String> {
        return this.value.split(",")
    }

    init {
        validate(value)
    }

    companion object {
        private val PATTERN = Regex("^ROLE_[A-Z0-9_]+(,ROLE_[A-Z0-9_]+)*$")

        fun validate(value: String) {
            require(PATTERN.matches(value)) {
                "Roles must be a comma-separated list of ROLE_xxx values (e.g., ROLE_ADMIN,ROLE_USER)."
            }
        }
    }
}