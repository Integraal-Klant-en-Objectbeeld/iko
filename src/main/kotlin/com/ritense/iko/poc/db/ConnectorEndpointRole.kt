package com.ritense.iko.poc.db

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "connector_endpoint_role")
class ConnectorEndpointRole(
    @Id
    var id: UUID,

    @ManyToOne
    @JoinColumn(name = "connector_endpoint_id")
    var connectorEndpoint: ConnectorEndpoint,

    @ManyToOne
    @JoinColumn(name = "connector_instance_id")
    var connectorInstance: ConnectorInstance,

    @Column(name = "role")
    var role: String,
)