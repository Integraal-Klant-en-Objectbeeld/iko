package com.ritense.iko.profile

import com.ritense.iko.mvc.model.AddProfileForm
import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.EditProfileForm
import com.ritense.iko.mvc.model.EditRelationForm
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(name = "profile", uniqueConstraints = [UniqueConstraint(columnNames = ["id", "name"])])
class Profile(
    @Id
    val id: UUID,

    @Column(name = "name", unique = true)
    var name: String,

    @Column(name = "primary_search")
    var primarySearch: UUID,

    @OneToMany(
        cascade = [(CascadeType.ALL)],
        fetch = FetchType.EAGER,
        orphanRemoval = true,
        mappedBy = "profile"
    )
    var relations: MutableList<Relation> = mutableListOf(),

    @Embedded
    var transform: Transform
) {

    fun handle(request: EditProfileForm) {
        this.primarySearch = UUID.fromString(request.primarySearch)
        this.name = request.name
        this.transform = Transform(request.transform)
    }

    fun addRelation(request: AddRelationForm) {
        this.relations.add(
            Relation(
                profile = this,
                sourceId = if (request.sourceId?.isNotBlank() == true) {
                    UUID.fromString(request.sourceId)
                } else {
                    null
                },
                searchId = request.searchId,
                transform = Transform(request.transform),
                sourceToSearchMapping = request.sourceToSearchMapping
            )
        )
    }

    fun changeRelation(request: EditRelationForm) {
        this.relations.removeIf { it.id == request.id }
        this.relations.add(
            Relation(
                id = request.id,
                profile = this,
                sourceId = if (request.sourceId?.isNotBlank() == true) {
                    UUID.fromString(request.sourceId)
                } else {
                    null
                },
                searchId = request.searchId,
                transform = Transform(request.transform),
                sourceToSearchMapping = request.sourceToSearchMapping
            )
        )
    }

    companion object {

        fun create(form: AddProfileForm) = Profile(
            id = UUID.randomUUID(),
            name = form.name,
            primarySearch = UUID.fromString(form.primarySearch),
            transform = Transform(form.transform),
        )

    }
}