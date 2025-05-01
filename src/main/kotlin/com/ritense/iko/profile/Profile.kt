package com.ritense.iko.profile

import com.ritense.iko.mvc.model.AddProfileForm
import com.ritense.iko.mvc.model.EditProfileForm
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
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
    val id: UUID,

    @Column(name = "name")
    var name: String,

    @Column(name = "primary_source")
    var primarySource: String,

    @OneToMany(cascade = [(CascadeType.ALL)], fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "profile")
    var relations: MutableList<Relation> = mutableListOf(),

    @Embedded
    var transform: Transform
) {

    fun handle(request: EditProfileForm) {
        this.name = request.name
        this.transform = Transform(request.transform)
    }

    companion object {

        fun create(request: AddProfileForm) = Profile(
            id = UUID.randomUUID(),
            name = request.name,
            primarySource = "TODO", // TODO
            transform = Transform(request.transform),
        )

    }
}