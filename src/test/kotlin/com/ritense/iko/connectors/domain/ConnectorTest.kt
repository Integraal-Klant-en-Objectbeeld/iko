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

package com.ritense.iko.connectors.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ConnectorTest {

    @Test
    fun `default isActive is false`() {
        val connector = createConnector()
        assertThat(connector.isActive).isFalse()
    }

    @Test
    fun `createNewVersion returns connector with isActive false`() {
        val connector = createConnector()
        connector.isActive = true

        val newVersion = connector.createNewVersion("2.0.0")

        assertThat(newVersion.isActive).isFalse()
        assertThat(newVersion.version.value).isEqualTo("2.0.0")
        assertThat(newVersion.tag).isEqualTo(connector.tag)
        assertThat(newVersion.name).isEqualTo(connector.name)
        assertThat(newVersion.connectorCode).isEqualTo(connector.connectorCode)
        assertThat(newVersion.id).isNotEqualTo(connector.id)
    }

    private fun createConnector(): Connector = Connector(
        id = UUID.randomUUID(),
        name = "Test Connector",
        tag = "test-tag",
        connectorCode = "# test yaml",
    )
}