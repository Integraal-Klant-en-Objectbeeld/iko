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
data class Profile(
    @Column(name = "id")
    @Id @GeneratedValue
    val id: UUID,

    @Column(name = "name")
    val name: String,

    @Column(name = "primary_source")
    val primarySource: String,

    @OneToMany(cascade = [(CascadeType.ALL)], fetch = FetchType.LAZY, orphanRemoval = true)
    val relations: List<Relation>,

    @Column(name = "transform")
    val transform: String
) {
}