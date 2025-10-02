package com.ritense.iko.poc.db

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "connector_instance")
class ConnectorInstance(
    @Id
    var id: UUID,

    @Column(name = "name")
    var name: String,

    @ManyToOne
    @JoinColumn(name = "connector_id")
    var connector: Connector,

    @Column(name = "tag")
    var tag: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @Convert(attributeName = "value", converter = AesGcmStringAttributeConverter::class)
    @CollectionTable(
        name = "connector_instance_config",
        joinColumns = [JoinColumn(name = "connector_instance_id")]
    )
    var config: Map<String, String>
)