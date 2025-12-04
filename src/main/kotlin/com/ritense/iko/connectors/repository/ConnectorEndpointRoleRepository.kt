package com.ritense.iko.connectors.repository

import com.ritense.iko.connectors.domain.ConnectorEndpoint
import com.ritense.iko.connectors.domain.ConnectorEndpointRole
import com.ritense.iko.connectors.domain.ConnectorInstance
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConnectorEndpointRoleRepository : JpaRepository<ConnectorEndpointRole, UUID> {
    fun findAllByConnectorInstance(connectorInstance: ConnectorInstance): List<ConnectorEndpointRole>

    fun findByConnectorEndpointAndConnectorInstance(
        connectorEndpoint: ConnectorEndpoint,
        connectorInstance: ConnectorInstance,
    ): List<ConnectorEndpointRole>
}