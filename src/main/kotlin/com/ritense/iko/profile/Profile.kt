package com.ritense.iko.profile

import com.ritense.iko.mvc.model.CreateProfileRequest
import com.ritense.iko.mvc.model.CreateRelationRequest
import com.ritense.iko.mvc.model.EditRelationRequest
import com.ritense.iko.mvc.model.ModifyProfileRequest
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "profile")
class Profile(
    @Column(name = "id")
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name")
    var name: String = "",

    @Column(name = "primary_source")
    var primarySource: String = "",

    @OneToMany(cascade = [(CascadeType.ALL)], fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "profile")
    var relations: MutableList<Relation> = mutableListOf(),

    @Column(name = "transform")
    var transform: String = ""
) {

    fun handle(request: ModifyProfileRequest) {
        this.name = request.name
        this.transform = request.transform
    }

    fun addRelation(request: CreateRelationRequest) {
        this.relations.add(
            Relation(
                profile = this,
                sourceId =if (request.sourceId.isNotBlank()) { UUID.fromString(request.sourceId) } else { null },
                searchId = request.searchId,
                transform = request.transform,
                sourceToSearchMapping = request.sourceToSearchMapping
            )
        )
    }

    fun changeRelation(request: EditRelationRequest) {
        this.relations.removeIf { it.id == request.relationId }
        this.relations.add(
            Relation(
                id = request.relationId,
                profile = this,
                sourceId = if (request.sourceId.isNotBlank()) { UUID.fromString(request.sourceId) } else { null },
                searchId = request.searchId,
                transform = request.transform,
                sourceToSearchMapping = request.sourceToSearchMapping
            )
        )
    }

    companion object {

        fun create(request: CreateProfileRequest) = Profile(
            id = UUID.randomUUID(),
            name = request.name,
            transform = request.transform,
            primarySource = request.primarySource,
        )

    }
}