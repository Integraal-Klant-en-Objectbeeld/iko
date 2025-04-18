package com.ritense.iko.profile

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.util.UUID

@Entity
class Relation(

    @Id @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    @JoinColumn(name = "profile_id")
    @ManyToOne
    val profile: Profile = Profile(),

    @Column(name = "source_id")
    val sourceId: UUID? = null,

    @Column(name = "source_to_search_mapping")
    val sourceToSearchMapping: String = "",

    @Column(name = "search_id")
    val searchId: String = "",

    @Column(name = "transform")
    val transform: String = ""
) {

    fun sourceToSearchMappingAsMap(): Map<String, String> {
        return ObjectMapper().readValue(this.sourceToSearchMapping, Map::class.java) as Map<String, String>
    }

}