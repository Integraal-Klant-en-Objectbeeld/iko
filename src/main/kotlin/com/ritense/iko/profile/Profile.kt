package com.ritense.iko.profile

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "profile")
class Profile(
    @Column(name = "id")
    @Id @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name")
    var name: String = "",

    @Column(name = "primary_source")
    var primarySource: String = "",

    @OneToMany(cascade = [(CascadeType.ALL)], fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "profile")
    var relations: List<Relation> = mutableListOf(),

    @Column(name = "transform")
    var transform: String = ""
) {

}