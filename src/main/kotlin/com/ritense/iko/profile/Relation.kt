package com.ritense.iko.profile

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

    @JoinColumn(name = "profile_id")
    @ManyToOne
    var profile: Profile,

    @Column(name = "source_id")
    var sourceId: UUID? = null,

    @Column(name = "source_to_search_mapping")
    var sourceToSearchMapping: String = "",

    @Column(name = "search_id")
    var endpointId: String = "",

    @Embedded
    var transform: Transform
) {

    fun sourceToSearchMappingAsMap(): Map<String, String> {
        return ObjectMapper().readValue(this.sourceToSearchMapping, Map::class.java) as Map<String, String>
    }

}