package com.ritense.iko.aggregateddataprofile

import com.fasterxml.jackson.databind.ObjectMapper
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

    @Column(name = "source_id")
    var sourceId: UUID? = null,

    @Column(name = "source_to_search_mapping")
    var sourceToSearchMapping: String = "",

    @Column(name = "endpoint_id")
    var endpointId: String = "",

    @Embedded
    var transform: Transform
) {

    fun sourceToSearchMappingAsMap(): Map<String, String> {
        return ObjectMapper().readValue(this.sourceToSearchMapping, Map::class.java) as Map<String, String>
    }

}