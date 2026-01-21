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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class RolesTest {

    @Test
    fun `creates roles with single valid role`() {
        val roles = Roles("ROLE_ADMIN")

        assertThat(roles.value).isEqualTo("ROLE_ADMIN")
    }

    @Test
    fun `creates roles with multiple valid roles`() {
        val roles = Roles("ROLE_ADMIN,ROLE_USER")

        assertThat(roles.value).isEqualTo("ROLE_ADMIN,ROLE_USER")
    }

    @Test
    fun `creates roles with underscores and hyphens`() {
        val roles = Roles("ROLE_ADMIN_USER,ROLE-TEST")

        assertThat(roles.value).isEqualTo("ROLE_ADMIN_USER,ROLE-TEST")
    }

    @Test
    fun `creates roles with alphanumeric characters`() {
        val roles = Roles("ROLE123,USER456")

        assertThat(roles.value).isEqualTo("ROLE123,USER456")
    }

    @Test
    fun `asList returns single role as list`() {
        val roles = Roles("ROLE_ADMIN")

        assertThat(roles.asList()).containsExactly("ROLE_ADMIN")
    }

    @Test
    fun `asList returns multiple roles as list`() {
        val roles = Roles("ROLE_ADMIN,ROLE_USER,ROLE_GUEST")

        assertThat(roles.asList()).containsExactly("ROLE_ADMIN", "ROLE_USER", "ROLE_GUEST")
    }

    @Test
    fun `throws exception for empty string`() {
        assertThatThrownBy { Roles("") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Roles must be a comma-separated list of values")
    }

    @Test
    fun `throws exception for roles with spaces`() {
        assertThatThrownBy { Roles("ROLE ADMIN") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Roles must be a comma-separated list of values")
    }

    @Test
    fun `throws exception for roles with leading comma`() {
        assertThatThrownBy { Roles(",ROLE_ADMIN") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Roles must be a comma-separated list of values")
    }

    @Test
    fun `throws exception for roles with trailing comma`() {
        assertThatThrownBy { Roles("ROLE_ADMIN,") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Roles must be a comma-separated list of values")
    }

    @Test
    fun `throws exception for roles with double comma`() {
        assertThatThrownBy { Roles("ROLE_ADMIN,,ROLE_USER") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Roles must be a comma-separated list of values")
    }

    @Test
    fun `throws exception for roles with special characters`() {
        assertThatThrownBy { Roles("ROLE@ADMIN") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Roles must be a comma-separated list of values")
    }
}