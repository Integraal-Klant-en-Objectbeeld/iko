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

class ConnectorEndpointRoleTest {

    @Test
    fun `create builds connector endpoint role with generated id`() {
        val connector = createConnector()
        val connectorInstance = createConnectorInstance(connector)
        val connectorEndpoint = createConnectorEndpoint(connector)
        val role = "ROLE_ADMIN"

        val connectorEndpointRole = ConnectorEndpointRole.create(
            connectorInstance = connectorInstance,
            connectorEndpoint = connectorEndpoint,
            role = role,
        )

        assertThat(connectorEndpointRole.id).isNotNull()
        assertThat(connectorEndpointRole.connectorInstance).isSameAs(connectorInstance)
        assertThat(connectorEndpointRole.connectorEndpoint).isSameAs(connectorEndpoint)
        assertThat(connectorEndpointRole.role).isEqualTo(role)
    }

    @Test
    fun `create generates unique id for each call`() {
        val connector = createConnector()
        val connectorInstance = createConnectorInstance(connector)
        val connectorEndpoint = createConnectorEndpoint(connector)

        val role1 = ConnectorEndpointRole.create(
            connectorInstance = connectorInstance,
            connectorEndpoint = connectorEndpoint,
            role = "ROLE_ADMIN",
        )
        val role2 = ConnectorEndpointRole.create(
            connectorInstance = connectorInstance,
            connectorEndpoint = connectorEndpoint,
            role = "ROLE_USER",
        )

        assertThat(role1.id).isNotEqualTo(role2.id)
    }

    @Test
    fun `create preserves role string exactly`() {
        val connector = createConnector()
        val connectorInstance = createConnectorInstance(connector)
        val connectorEndpoint = createConnectorEndpoint(connector)
        val roleValue = "ROLE_CUSTOM_123"

        val connectorEndpointRole = ConnectorEndpointRole.create(
            connectorInstance = connectorInstance,
            connectorEndpoint = connectorEndpoint,
            role = roleValue,
        )

        assertThat(connectorEndpointRole.role).isEqualTo(roleValue)
    }

    private fun createConnector(): Connector = Connector(
        id = UUID.randomUUID(),
        name = "Test Connector",
        tag = "test-tag",
        connectorCode = "TEST_CODE",
    )

    private fun createConnectorInstance(connector: Connector): ConnectorInstance = ConnectorInstance(
        id = UUID.randomUUID(),
        name = "Test Instance",
        connector = connector,
        tag = "test-instance-tag",
        config = emptyMap(),
    )

    private fun createConnectorEndpoint(connector: Connector): ConnectorEndpoint = ConnectorEndpoint(
        id = UUID.randomUUID(),
        name = "Test Endpoint",
        connector = connector,
        operation = "GET",
    )
}