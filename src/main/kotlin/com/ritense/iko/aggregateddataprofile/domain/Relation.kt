package com.ritense.iko.aggregateddataprofile.domain

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.util.UUID

@Entity
class Relation(
    @Id
    val id: UUID = UUID.randomUUID(),
    @JoinColumn(name = "aggregated_data_profile_id")
    @ManyToOne
    var aggregatedDataProfile: AggregatedDataProfile,
    @Column(name = "property_name")
    var propertyName: String,
    @Column(name = "source_id")
    var sourceId: UUID? = null,
    @Embedded
    var sourceToEndpointMapping: EndpointTransform = EndpointTransform("{}"),
    @Column(name = "connector_instance_id")
    var connectorInstanceId: UUID,
    @Column(name = "connector_endpoint_id")
    var connectorEndpointId: UUID,
    @Embedded
    var resultTransform: Transform = Transform("."),
    @Embedded
    var relationCacheSettings: RelationCacheSettings,
)