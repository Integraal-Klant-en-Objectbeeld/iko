package com.ritense.iko.aggregateddataprofile.domain

import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.AggregatedDataProfileAddForm
import com.ritense.iko.mvc.model.AggregatedDataProfileEditForm
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
    uniqueConstraints = [UniqueConstraint(columnNames = ["id", "name"])],
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

    @Embedded
    var endpointTransform: EndpointTransform,

    @OneToMany(
        cascade = [(CascadeType.ALL)],
        fetch = FetchType.EAGER,
        orphanRemoval = true,
        mappedBy = "aggregatedDataProfile",
    )
    var relations: MutableList<Relation> = mutableListOf(),

    @Embedded
    var resultTransform: Transform,

    @Column(name = "role")
    var role: String? = null,

    @Embedded
    var aggregatedDataProfileCacheSetting: AggregatedDataProfileCacheSetting,
) {

    fun handle(request: AggregatedDataProfileEditForm) {
        if (!request.role.isNullOrBlank()) {
            this.role = request.role
        } else {
            val sanitizedName = request.name.replace(Regex("[^0-9a-zA-Z_-]+"), "")
            val defaultRole = "ROLE_AGGREGATED_DATA_PROFILE_${sanitizedName.uppercase()}"
            this.role = defaultRole
        }
        this.connectorEndpointId = request.connectorEndpointId
        this.connectorInstanceId = request.connectorInstanceId
        this.endpointTransform = EndpointTransform(request.endpointTransform)
        this.resultTransform = Transform(request.resultTransform)
        this.aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(
            enabled = request.cacheEnabled,
            timeToLive = request.cacheTimeToLive,
        )
    }

    fun addRelation(request: AddRelationForm) {
        this.relations.add(
            Relation(
                aggregatedDataProfile = this,
                sourceId = request.sourceId.takeUnless { it.isNullOrBlank() }?.let { UUID.fromString(it) },
                resultTransform = Transform(requireNotNull(request.resultTransform) { "Result Transform is required." }),
                sourceToEndpointMapping = requireNotNull(request.sourceToEndpointMapping) { "Source to endpoint mapping is required." },
                connectorEndpointId = requireNotNull(request.connectorEndpointId) { "Connector endpoint is required." },
                connectorInstanceId = requireNotNull(request.connectorInstanceId) { "Connector instance is required." },
                propertyName = requireNotNull(request.propertyName) { "Property name is required." },
                relationCacheSettings = RelationCacheSettings(),
            ),
        )
    }

    fun changeRelation(request: EditRelationForm) {
        this.relations.removeIf { it.id == request.id }
        this.relations.add(
            Relation(
                id = request.id,
                aggregatedDataProfile = this,
                sourceId = request.sourceId.takeUnless { it.isNullOrBlank() }?.let { UUID.fromString(it) },
                resultTransform = Transform(request.resultTransform),
                sourceToEndpointMapping = request.sourceToEndpointMapping,
                connectorInstanceId = request.connectorInstanceId,
                connectorEndpointId = request.connectorEndpointId,
                propertyName = request.propertyName,
                relationCacheSettings = RelationCacheSettings(),
            ),
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
        fun create(form: AggregatedDataProfileAddForm): AggregatedDataProfile {
            val sanitizedName = form.name.replace(Regex("[^0-9a-zA-Z_-]+"), "")
            val defaultRole = "ROLE_AGGREGATED_DATA_PROFILE_${sanitizedName.uppercase()}"
            val role = if (form.role.isBlank()) defaultRole else form.role
            return AggregatedDataProfile(
                id = UUID.randomUUID(),
                name = form.name,
                role = role,
                connectorInstanceId = requireNotNull(form.connectorInstanceId) { "Connector instance is required." },
                connectorEndpointId = requireNotNull(form.connectorEndpointId) { "Connector endpoint is required." },
                endpointTransform = EndpointTransform(requireNotNull(form.endpointTransform) { "Endpoint Transform is required." }),
                resultTransform = Transform(requireNotNull(form.resultTransform) { "Transform is required." }),
                aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
            )
        }
    }
}