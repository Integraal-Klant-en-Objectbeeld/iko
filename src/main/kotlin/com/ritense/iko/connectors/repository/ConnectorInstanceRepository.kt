package com.ritense.iko.connectors.repository

import com.ritense.iko.connectors.domain.Connector
import com.ritense.iko.connectors.domain.ConnectorInstance
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConnectorInstanceRepository : JpaRepository<ConnectorInstance, UUID> {
    fun findByConnector(connector: Connector): List<ConnectorInstance>
    fun findByConnectorTagAndTag(tag: String, config: String): ConnectorInstance?
}