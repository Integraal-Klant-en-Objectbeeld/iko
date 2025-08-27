package com.ritense.iko.poc.db

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConnectorRepository : JpaRepository<Connector, UUID> {
}