package com.ritense.iko.poc.db

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "connector")
class Connector(
    @Id
    var id: UUID,

    @Column(name = "name")
    var name: String,

    @Column(name = "description")
    var description: String,

    @Column(name = "tag")
    var tag: String,

    @Column(name = "connector_code")
    var connectorCode: String,

)