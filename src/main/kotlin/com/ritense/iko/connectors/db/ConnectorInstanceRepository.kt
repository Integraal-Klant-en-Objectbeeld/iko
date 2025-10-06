package com.ritense.iko.connectors.db

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConnectorInstanceRepository : JpaRepository<ConnectorInstance, UUID> {
    fun findByConnector(connector: Connector): List<ConnectorInstance>
    fun findByConnectorTagAndTag(tag: String, config: String): ConnectorInstance?
}