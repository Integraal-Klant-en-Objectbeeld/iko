package com.ritense.iko.connectors.repository

import com.ritense.iko.connectors.domain.Connector
import com.ritense.iko.connectors.domain.ConnectorEndpoint
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConnectorEndpointRepository : JpaRepository<ConnectorEndpoint, UUID> {
    fun findByConnector(connector: Connector): List<ConnectorEndpoint>

    fun findByConnectorTagAndOperation(
        tag: String,
        operation: String,
    ): ConnectorEndpoint?
}