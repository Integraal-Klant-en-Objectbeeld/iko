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

package com.ritense.iko.aggregateddataprofile.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Version(
    @Column(name = "version")
    val value: String,
) {
    init {
        validate(value)
    }

    companion object {
        // Semver regex: major.minor.patch (e.g., 1.0.0, 2.1.3)
        private val SEMVER_PATTERN = Regex("""^\d+\.\d+\.\d+$""")

        fun validate(version: String) {
            require(SEMVER_PATTERN.matches(version)) {
                "Version must be in semver format (e.g., 1.0.0), got: $version"
            }
        }

        /**
         * Check if a version string is valid without throwing an exception.
         * Useful for validators.
         */
        fun isValid(version: String?): Boolean {
            if (version.isNullOrBlank()) return false
            return SEMVER_PATTERN.matches(version)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Version
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value
}
