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

package com.ritense.iko.mvc.controller

import com.ritense.iko.connectors.domain.Connector
import com.ritense.iko.connectors.domain.ConnectorInstance
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorEndpointRoleRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import com.ritense.iko.connectors.repository.ConnectorRepository
import com.ritense.iko.connectors.service.ConnectorService
import com.ritense.iko.mvc.model.connector.ConnectorInstanceEditForm
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.validation.BeanPropertyBindingResult
import java.util.Optional
import java.util.UUID

class ConnectorControllerInstanceTest {

    private val connectorRepository = mock<ConnectorRepository>()
    private val connectorInstanceRepository = mock<ConnectorInstanceRepository>()
    private val connectorEndpointRepository = mock<ConnectorEndpointRepository>()
    private val connectorEndpointRoleRepository = mock<ConnectorEndpointRoleRepository>()
    private val connectorService = mock<ConnectorService>()

    private val controller = ConnectorController(
        connectorRepository = connectorRepository,
        connectorInstanceRepository = connectorInstanceRepository,
        connectorEndpointRepository = connectorEndpointRepository,
        connectorEndpointRoleRepository = connectorEndpointRoleRepository,
        connectorService = connectorService,
    )

    @Test
    fun `editConnectorInstance throws when connector is FINAL`() {
        val connector = Connector(
            id = UUID.randomUUID(),
            name = "Test Connector",
            tag = "test-tag",
            connectorCode = "# test yaml",
        )
        connector.finalize()
        val connectorInstance = ConnectorInstance(
            id = UUID.randomUUID(),
            name = "Test Instance",
            connector = connector,
            tag = "test-instance-tag",
            config = emptyMap(),
        )
        whenever(connectorRepository.findById(connector.id)).thenReturn(Optional.of(connector))
        whenever(connectorInstanceRepository.findById(connectorInstance.id)).thenReturn(Optional.of(connectorInstance))

        val form = ConnectorInstanceEditForm(name = "New Name", reference = "new-ref")
        val bindingResult = BeanPropertyBindingResult(form, "form")

        assertThatThrownBy {
            controller.editConnectorInstance(connector.id, connectorInstance.id, form, bindingResult)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Cannot modify a FINAL version")
    }
}