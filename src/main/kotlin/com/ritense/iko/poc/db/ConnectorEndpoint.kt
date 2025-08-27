package com.ritense.iko.poc.db

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "connector_endpoint")
class ConnectorEndpoint(
    @Id
    var id: UUID,

    @Column(name = "name")
    var name: String,

    @Column(name = "description")
    var description: String,

    @ManyToOne
    @JoinColumn(name = "connector_id")
    var connector: Connector,

    @Column(name = "operation")
    var operation: String,
)