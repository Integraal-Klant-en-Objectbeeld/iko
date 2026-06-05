/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
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

package com.ritense.iko.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import java.time.Duration

class AdminSessionPropertiesTest {
    @Test
    fun `binds duration values onto an existing instance via setters`() {
        // Mirrors how Spring binds the @Component-registered bean: it binds onto
        // an already-constructed instance, which requires setters (var), not
        // constructor binding. Guards against the "No setter found" boot failure.
        val source = MapConfigurationPropertySource(
            mapOf(
                "iko.security.admin.session.timeout" to "1m",
                "iko.security.admin.session.warning-before" to "30s",
            ),
        )

        val properties = AdminSessionProperties()
        Binder(source).bind(
            "iko.security.admin.session",
            Bindable.ofInstance(properties),
        )

        assertThat(properties.timeout).isEqualTo(Duration.ofMinutes(1))
        assertThat(properties.warningBefore).isEqualTo(Duration.ofSeconds(30))
        assertThat(properties.timeoutSeconds).isEqualTo(60)
        assertThat(properties.warningBeforeSeconds).isEqualTo(30)
    }

    @Test
    fun `uses defaults when no configuration is provided`() {
        val properties = Binder(MapConfigurationPropertySource(emptyMap<String, Any>()))
            .bindOrCreate("iko.security.admin.session", AdminSessionProperties::class.java)

        assertThat(properties.timeout).isEqualTo(Duration.ofMinutes(30))
        assertThat(properties.warningBefore).isEqualTo(Duration.ofMinutes(2))
    }
}