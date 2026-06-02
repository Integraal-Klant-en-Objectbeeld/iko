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

package com.ritense.iko.connectors.service

import com.ritense.iko.connectors.domain.Connector
import com.ritense.iko.connectors.domain.Version
import org.apache.camel.impl.DefaultCamelContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.util.UUID

class ConnectorServiceTest {

    @Nested
    inner class NamespaceUri {

        @Test
        fun `namespaces connector from URI with version`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:connector:my-tag",
                "1.0.0",
            )
            assertThat(result).isEqualTo("direct:iko:connector:my-tag:1.0.0")
        }

        @Test
        fun `namespaces transform from URI with version`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:endpoint:transform:my-tag",
                "2.0.0",
            )
            assertThat(result).isEqualTo("direct:iko:endpoint:transform:my-tag:2.0.0")
        }

        @Test
        fun `namespaces transform from URI with operation and version`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:endpoint:transform:my-tag.get_zaak",
                "1.0.0",
            )
            assertThat(result).isEqualTo("direct:iko:endpoint:transform:my-tag:1.0.0.get_zaak")
        }

        @Test
        fun `does not modify unrelated URIs`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:config",
                "1.0.0",
            )
            assertThat(result).isEqualTo("direct:iko:config")
        }

        @Test
        fun `does not modify already-namespaced connector URIs`() {
            val result = ConnectorService.namespaceUri(
                "direct:iko:connector:my-tag:1.0.0",
                "2.0.0",
            )
            assertThat(result).isEqualTo("direct:iko:connector:my-tag:1.0.0")
        }
    }

    @Nested
    inner class LoadConnectorRoutes {

        private lateinit var camelContext: DefaultCamelContext
        private lateinit var service: ConnectorService

        @BeforeEach
        fun setUp() {
            camelContext = DefaultCamelContext()
            camelContext.start()
            service = ConnectorService(
                connectorRepository = mock(),
                connectorInstanceRepository = mock(),
                connectorEndpointRepository = mock(),
                connectorEndpointRoleRepository = mock(),
                camelContext = camelContext,
            )
        }

        @AfterEach
        fun tearDown() {
            camelContext.stop()
        }

        @Test
        fun `route ids are namespaced with tag and version`() {
            val connector = connector(tag = "test", version = "1.0.0")

            service.loadConnectorRoutes(connector)

            val routeIds = camelContext.routes.map { it.routeId }
            assertThat(routeIds).contains(
                "connector:test:1.0.0:brp-wsgateway-personen",
                "connector:test:1.0.0:brp-wsgateway-personen-transform",
            )
        }

        @Test
        fun `loading two versions of same connector coexists without route id collision`() {
            val v100 = connector(tag = "test", version = "1.0.0")
            val v101 = connector(tag = "test", version = "1.0.1")

            service.loadConnectorRoutes(v100)
            service.loadConnectorRoutes(v101)

            val routeIds = camelContext.routes.map { it.routeId }
            assertThat(routeIds).contains(
                "connector:test:1.0.0:brp-wsgateway-personen",
                "connector:test:1.0.0:brp-wsgateway-personen-transform",
                "connector:test:1.0.1:brp-wsgateway-personen",
                "connector:test:1.0.1:brp-wsgateway-personen-transform",
            )

            assertThat(camelContext.hasEndpoint("direct://iko:endpoint:transform:test:1.0.0.Personen")).isNotNull()
            assertThat(camelContext.hasEndpoint("direct://iko:endpoint:transform:test:1.0.1.Personen")).isNotNull()
        }

        @Test
        fun `loading the same connector twice is idempotent`() {
            val connector = connector(tag = "test", version = "1.0.0")

            service.loadConnectorRoutes(connector)
            val routeCountAfterFirst = camelContext.routes.size

            service.loadConnectorRoutes(connector)

            assertThat(camelContext.routes.size).isEqualTo(routeCountAfterFirst)
        }

        private fun connector(tag: String, version: String): Connector = Connector(
            id = UUID.randomUUID(),
            name = tag,
            tag = tag,
            version = Version(version),
            connectorCode = CONNECTOR_YAML.trimIndent(),
        )
    }

    companion object {
        // Minimal YAML with one connector route and one endpoint transform route.
        // Route ids match what's seen in the issue #282 logs.
        private const val CONNECTOR_YAML = """
            - route:
                id: brp-wsgateway-personen
                from:
                  uri: "direct:iko:connector:test"
                  steps:
                    - setBody:
                        constant: '{}'
            - route:
                id: brp-wsgateway-personen-transform
                from:
                  uri: "direct:iko:endpoint:transform:test.Personen"
                  steps:
                    - setBody:
                        constant: '{}'
        """
    }
}