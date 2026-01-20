package com.ritense.iko.aggregateddataprofile.domain

import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Validation.ROLES_PATTERN
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
        private val PATTERN = Regex(ROLES_PATTERN)

        fun validate(value: String) {
            require(PATTERN.matches(value)) {
                "Roles must be a comma-separated list of values (e.g., ROLE_ADMIN,ROLE_USER)."
            }
        }
    }
}