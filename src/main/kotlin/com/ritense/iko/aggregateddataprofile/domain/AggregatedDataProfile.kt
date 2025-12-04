package com.ritense.iko.aggregateddataprofile.domain

import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.AggregatedDataProfileForm
import com.ritense.iko.mvc.model.DeleteRelationForm
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

    @Column(name = "connector_instance_id")
    var connectorInstanceId: UUID,

    @Column(name = "connector_endpoint_id")
    var connectorEndpointId: UUID,

    @OneToMany(
        cascade = [(CascadeType.ALL)],
        fetch = FetchType.EAGER,
        orphanRemoval = true,
        mappedBy = "aggregatedDataProfile"
    )
    var relations: MutableList<Relation> = mutableListOf(),

    @Embedded
    var transform: Transform,

    @Column(name = "role")
    var role: String? = null,

    @Embedded
    var aggregatedDataProfileCacheSetting: AggregatedDataProfileCacheSetting
) {

    fun handle(request: AggregatedDataProfileForm) {
        this.name = request.name
        if (!request.role.isNullOrBlank()) {
            this.role = request.role
        } else {
            val sanitizedName = request.name.replace(Regex("[^0-9a-zA-Z_-]+"), "")
            val defaultRole = "ROLE_AGGREGATED_DATA_PROFILE_${sanitizedName.uppercase()}"
            this.role = defaultRole
        }
        this.connectorEndpointId = request.connectorEndpointId
        this.connectorInstanceId = request.connectorInstanceId
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
                transform = Transform(request.transform),
                sourceToEndpointMapping = request.sourceToEndpointMapping,
                connectorEndpointId = request.connectorEndpointId,
                connectorInstanceId = request.connectorInstanceId,
                propertyName = request.propertyName,
                relationCacheSettings = RelationCacheSettings()
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
                transform = Transform(request.transform),
                sourceToEndpointMapping = request.sourceToEndpointMapping,
                connectorInstanceId = request.connectorInstanceId,
                connectorEndpointId = request.connectorEndpointId,
                propertyName = request.propertyName,
                relationCacheSettings = RelationCacheSettings()
            )
        )
    }

    fun removeRelation(request: DeleteRelationForm) {
        // Remove the selected relation and all its descendants
        val toRemove: MutableSet<UUID> = linkedSetOf()

        fun collectDescendants(parentId: UUID) {
            toRemove += parentId
            val children = this.relations.filter { it.sourceId == parentId }
            children.forEach { collectDescendants(it.id) }
        }
        collectDescendants(request.id)
        this.relations.removeIf { it.id in toRemove }
    }

    companion object {
        fun create(form: AggregatedDataProfileForm): AggregatedDataProfile {
            val sanitizedName = form.name.replace(Regex("[^0-9a-zA-Z_-]+"), "")
            val defaultRole = "ROLE_AGGREGATED_DATA_PROFILE_${sanitizedName.uppercase()}"
            val role = if (form.role.isNullOrBlank()) defaultRole else form.role
            return AggregatedDataProfile(
                id = UUID.randomUUID(),
                name = form.name,
                role = role,
                transform = Transform(form.transform),
                connectorEndpointId = form.connectorEndpointId,
                connectorInstanceId = form.connectorInstanceId,
                aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting()
            )
        }
    }

}