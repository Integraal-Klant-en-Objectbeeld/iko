package com.ritense.iko.connectors.repository

import com.ritense.iko.connectors.domain.Connector
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConnectorRepository : JpaRepository<Connector, UUID>