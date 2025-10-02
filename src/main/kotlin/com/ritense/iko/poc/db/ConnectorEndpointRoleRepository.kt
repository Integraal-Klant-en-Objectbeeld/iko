package com.ritense.iko.poc.db

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConnectorEndpointRoleRepository : JpaRepository<ConnectorEndpointRole, UUID> {
    fun findAllByConnectorInstance(connectorInstance: ConnectorInstance): List<ConnectorEndpointRole>
    fun findByConnectorEndpointAndConnectorInstance(connectorEndpoint: ConnectorEndpoint, connectorInstance: ConnectorInstance): List<ConnectorEndpointRole>
}