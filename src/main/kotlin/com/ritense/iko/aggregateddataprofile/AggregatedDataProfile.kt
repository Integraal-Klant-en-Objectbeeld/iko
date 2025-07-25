package com.ritense.iko.aggregateddataprofile

import com.ritense.iko.mvc.model.AddAggregatedDataProfileForm
import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.DeleteRelationForm
import com.ritense.iko.mvc.model.EditAggregatedDataProfileForm
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
@Table(
    name = "aggregated_data_profile",
    uniqueConstraints = [UniqueConstraint(columnNames = ["id", "name"])]
)
class AggregatedDataProfile(
    @Id
    val id: UUID,

    @Column(name = "name", unique = true)
    var name: String,

    @Column(name = "primary_endpoint")
    var primaryEndpoint: UUID,

    @OneToMany(
        cascade = [(CascadeType.ALL)],
        fetch = FetchType.EAGER,
        orphanRemoval = true,
        mappedBy = "aggregatedDataProfile"
    )
    var relations: MutableList<Relation> = mutableListOf(),

    @Embedded
    var transform: Transform
) {

    fun handle(request: EditAggregatedDataProfileForm) {
        this.primaryEndpoint = UUID.fromString(request.primaryEndpoint)
        this.name = request.name
        this.transform = Transform(request.transform)
    }

    fun addRelation(request: AddRelationForm) {
        this.relations.add(
            Relation(
                aggregatedDataProfile = this,
                sourceId = if (request.sourceId?.isNotBlank() == true) {
                    UUID.fromString(request.sourceId)
                } else {
                    null
                },
                endpointId = request.endpointId,
                transform = Transform(request.transform),
                sourceToEndpointMapping = request.sourceToEndpointMapping
            )
        )
    }

    fun changeRelation(request: EditRelationForm) {
        this.relations.removeIf { it.id == request.id }
        this.relations.add(
            Relation(
                id = request.id,
                aggregatedDataProfile = this,
                sourceId = if (request.sourceId?.isNotBlank() == true) {
                    UUID.fromString(request.sourceId)
                } else {
                    null
                },
                endpointId = request.endpointId,
                transform = Transform(request.transform),
                sourceToEndpointMapping = request.sourceToEndpointMapping
            )
        )
    }

    fun removeRelation(request: DeleteRelationForm) {
        this.relations.removeIf { it.id == request.id }
    }

    companion object {
        fun create(form: AddAggregatedDataProfileForm) = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = form.name,
            primaryEndpoint = UUID.fromString(form.primaryEndpoint),
            transform = Transform(form.transform)
        )
    }

}