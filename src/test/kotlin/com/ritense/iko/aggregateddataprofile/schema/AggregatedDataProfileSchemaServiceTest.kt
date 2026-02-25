/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.aggregateddataprofile.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfileCacheSetting
import com.ritense.iko.aggregateddataprofile.domain.EndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.aggregateddataprofile.domain.RelationCacheSettings
import com.ritense.iko.aggregateddataprofile.domain.RelationEndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Roles
import com.ritense.iko.aggregateddataprofile.domain.Transform
import com.ritense.iko.aggregateddataprofile.domain.Version
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.connectors.domain.Connector
import com.ritense.iko.connectors.domain.ConnectorEndpoint
import com.ritense.iko.connectors.domain.ConnectorInstance
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

class AggregatedDataProfileSchemaServiceTest {

    private val mapper = ObjectMapper()

    private lateinit var aggregatedDataProfileRepository: AggregatedDataProfileRepository
    private lateinit var connectorInstanceRepository: ConnectorInstanceRepository
    private lateinit var connectorEndpointRepository: ConnectorEndpointRepository
    private lateinit var service: AggregatedDataProfileSchemaService

    private val connector = Connector(
        id = UUID.randomUUID(),
        name = "pet-connector",
        tag = "pet",
        connectorCode = "",
    )

    private val connectorInstance = ConnectorInstance(
        id = UUID.randomUUID(),
        name = "pet-instance",
        connector = connector,
        tag = "pet-tag",
        config = mapOf(
            "specificationUri" to "classpath:pet-api.yaml",
            "host" to "http://localhost:10000",
        ),
    )

    private val petsEndpoint = ConnectorEndpoint(
        id = UUID.randomUUID(),
        name = "GetPets",
        connector = connector,
        operation = "GetPets",
    )

    private val ownersEndpoint = ConnectorEndpoint(
        id = UUID.randomUUID(),
        name = "GetOwners",
        connector = connector,
        operation = "GetOwners",
    )

    @BeforeEach
    fun setUp() {
        aggregatedDataProfileRepository = mock()
        connectorInstanceRepository = mock()
        connectorEndpointRepository = mock()

        whenever(connectorInstanceRepository.findById(connectorInstance.id))
            .thenReturn(Optional.of(connectorInstance))
        whenever(connectorEndpointRepository.findById(petsEndpoint.id))
            .thenReturn(Optional.of(petsEndpoint))
        whenever(connectorEndpointRepository.findById(ownersEndpoint.id))
            .thenReturn(Optional.of(ownersEndpoint))

        service = AggregatedDataProfileSchemaService(
            aggregatedDataProfileRepository = aggregatedDataProfileRepository,
            connectorInstanceRepository = connectorInstanceRepository,
            connectorEndpointRepository = connectorEndpointRepository,
            openApiMockGenerator = OpenApiMockGenerator(mapper),
            jsonSchemaInferrer = JsonSchemaInferrer(mapper),
            mapper = mapper,
        )
    }

    @Test
    fun `generateSchema returns schema for ADP without relations`() {
        val adp = createAdp(resultTransform = ".[0] | {id: .id, name: .name}")

        val schema = service.generateSchema(adp)

        assertThat(schema).isNotNull()
        val schemaNode = mapper.readTree(schema)
        assertThat(schemaNode.get("\$schema").asText()).isEqualTo("https://json-schema.org/draft/2020-12/schema")
        assertThat(schemaNode.get("type").asText()).isEqualTo("object")
        assertThat(schemaNode.get("properties").has("id")).isTrue()
        assertThat(schemaNode.get("properties").has("name")).isTrue()
    }

    @Test
    fun `generateSchema returns schema for passthrough transform`() {
        val adp = createAdp(resultTransform = ".")

        val schema = service.generateSchema(adp)

        assertThat(schema).isNotNull()
        val schemaNode = mapper.readTree(schema)
        assertThat(schemaNode.get("type").asText()).isEqualTo("array")
        assertThat(schemaNode.get("items").get("properties").has("id")).isTrue()
        assertThat(schemaNode.get("items").get("properties").has("name")).isTrue()
        assertThat(schemaNode.get("items").get("properties").has("ownerId")).isTrue()
    }

    @Test
    fun `generateSchema returns null when connector has no specificationUri`() {
        val instanceWithoutSpec = ConnectorInstance(
            id = UUID.randomUUID(),
            name = "no-spec-instance",
            connector = connector,
            tag = "no-spec",
            config = mapOf("host" to "http://localhost:10000"),
        )
        whenever(connectorInstanceRepository.findById(instanceWithoutSpec.id))
            .thenReturn(Optional.of(instanceWithoutSpec))
        val adp = createAdp(
            connectorInstanceId = instanceWithoutSpec.id,
            resultTransform = ".",
        )

        val schema = service.generateSchema(adp)

        assertThat(schema).isNull()
    }

    @Test
    fun `generateSchema composes left-right input for ADP with relations`() {
        val adp = createAdp(resultTransform = "{pets: .left, owners: .right.owners}")
        val relation = Relation(
            aggregatedDataProfile = adp,
            propertyName = "owners",
            sourceId = adp.id,
            endpointTransform = RelationEndpointTransform("{\"id\": .source}"),
            connectorInstanceId = connectorInstance.id,
            connectorEndpointId = ownersEndpoint.id,
            resultTransform = Transform("."),
            relationCacheSettings = RelationCacheSettings(),
        )
        adp.relations.add(relation)

        val schema = service.generateSchema(adp)

        assertThat(schema).isNotNull()
        val schemaNode = mapper.readTree(schema)
        assertThat(schemaNode.get("type").asText()).isEqualTo("object")
        assertThat(schemaNode.get("properties").has("pets")).isTrue()
        assertThat(schemaNode.get("properties").has("owners")).isTrue()
    }

    @Test
    fun `generateSchema detects array mode for relation with array endpoint transform`() {
        val adp = createAdp(resultTransform = "{pets: .left, owners: .right.owners}")
        val relation = Relation(
            aggregatedDataProfile = adp,
            propertyName = "owners",
            sourceId = adp.id,
            endpointTransform = RelationEndpointTransform("[.source[] | {\"id\": .ownerId}]"),
            connectorInstanceId = connectorInstance.id,
            connectorEndpointId = ownersEndpoint.id,
            resultTransform = Transform("."),
            relationCacheSettings = RelationCacheSettings(),
        )
        adp.relations.add(relation)

        val schema = service.generateSchema(adp)

        assertThat(schema).isNotNull()
        val schemaNode = mapper.readTree(schema)
        val ownersProp = schemaNode.get("properties").get("owners")
        assertThat(ownersProp.get("type").asText()).isEqualTo("array")
    }

    private fun createAdp(
        connectorInstanceId: UUID = connectorInstance.id,
        resultTransform: String = ".",
    ): AggregatedDataProfile = AggregatedDataProfile(
        id = UUID.randomUUID(),
        name = "test-adp",
        version = Version("1.0.0"),
        isActive = true,
        connectorInstanceId = connectorInstanceId,
        connectorEndpointId = petsEndpoint.id,
        endpointTransform = EndpointTransform("."),
        resultTransform = Transform(resultTransform),
        roles = Roles("ROLE_TEST"),
        aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
    )
}