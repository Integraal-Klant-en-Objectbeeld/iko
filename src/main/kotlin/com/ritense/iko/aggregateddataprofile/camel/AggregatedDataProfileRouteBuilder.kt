package com.ritense.iko.aggregateddataprofile.camel

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.cache.processor.AdpCacheCheckProcessor
import com.ritense.iko.cache.processor.AdpCachePutProcessor
import com.ritense.iko.cache.processor.RelationCacheCheckProcessor
import com.ritense.iko.cache.processor.RelationCachePutProcessor
import com.ritense.iko.connectors.camel.Iko
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.builder.FlexibleAggregationStrategy
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jackson.JacksonConstants
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException

class AggregatedDataProfileRouteBuilder(
    private val camelContext: CamelContext,
    private val aggregatedDataProfile: AggregatedDataProfile,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val adpCacheCheckProcessor: AdpCacheCheckProcessor,
    private val adpCachePutProcessor: AdpCachePutProcessor,
    private val relationCacheCheckProcessor: RelationCacheCheckProcessor,
    private val relationCachePutProcessor: RelationCachePutProcessor,
) : RouteBuilder(camelContext) {

    fun createRelationRoute(aggregatedDataProfile: AggregatedDataProfile, source: Relation) {
        camelContext.globalOptions[JacksonConstants.ENABLE_TYPE_CONVERTER] = "true"

        val relations = aggregatedDataProfile.relations.filter { it.sourceId == source.id }
        val connectorInstance = connectorInstanceRepository.findById(source.connectorInstanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connectorEndpoint = connectorEndpointRepository.findById(source.connectorEndpointId)
            .orElseThrow { NoSuchElementException("Connector endpoint not found") }

        from("direct:relation_${source.id}")
            .routeId("relation_${source.id}_direct")
            .streamCache(true)
            .removeHeaders("*")
            .setVariable("endpointMapping").jq(source.sourceToEndpointMapping)
            .setVariable("relationId", constant(source.id))
            .marshal().json()
            .choice()
            .`when` { ex -> ex.getVariable("endpointMapping", JsonNode::class.java).isArray }
            .to("direct:relation_${source.id}_array")
            .`when` { ex -> ex.getVariable("endpointMapping", JsonNode::class.java).isObject }
            .to("direct:relation_${source.id}_map")

        from("direct:relation_${source.id}_map")
            .routeId("relation_${source.id}_map")
            .process {
                it.getVariable("endpointMapping", ObjectNode::class.java).forEachEntry { key, value ->
                    it.getIn().setHeader(key, value.asText())
                }
            }
            .removeVariable("endpointMapping")
            .to("direct:relation_${source.id}_loop")
            .transform(jq(source.transform.expression))
            .unmarshal().json()

        from("direct:relation_${source.id}_array")
            .routeId("relation_${source.id}_array")
            .split(variable("endpointMapping"), FlexibleAggregationStrategy<JsonNode>()
                    .pick(body())
                    .castAs(JsonNode::class.java)
                    .accumulateInCollection(ArrayList::class.java)
            )
            .parallelProcessing()
                .process { ex ->
                    ex.getIn().getBody(ObjectNode::class.java).forEachEntry { key, value ->
                        ex.getIn().setHeader(key, value.asText())
                    }
                }
                .removeVariable("endpointMapping")
                .to("direct:relation_${source.id}_loop") // Executes the relation route
            .end()
            .transform(jq(source.transform.expression))
            .unmarshal().json()

        // Executes each relation route
        from("direct:relation_${source.id}_loop")
            .routeId("relation_${source.id}_loop")
            .unmarshal().json()
            .setVariable("connector", constant(connectorInstance.connector.tag))
            .setVariable("config", constant(connectorInstance.tag))
            .setVariable("operation", constant(connectorEndpoint.operation))
            .setVariable("relationPropertyName", constant(source.propertyName))
            .setVariable("relationId", constant(source.id))
            .setVariable("endpointMapping").jq(source.sourceToEndpointMapping)
            .to(Iko.endpoint("validate"))
            .to(Iko.iko("config"))
            .to(Iko.transform())
            .streamCache(true)
            .process(relationCacheCheckProcessor)
            .choice()
                .`when` { ex -> !ex.getVariable("cacheHit_${source.id}", Boolean::class.java) } // cacheHit false
                    .to(Iko.connector())
                    .process(relationCachePutProcessor)
                .end()
            .let {
                if (relations.isNotEmpty()) {
                    it.enrich("direct:multicast_${source.id}", PairAggregator)
                } else {
                    it
                }
            }

        if (relations.isNotEmpty()) {
            var multicast = from("direct:multicast_${source.id}")
                .routeId("relation_${source.id}_multicast")
                .multicast(MapAggregator)
                .parallelProcessing()

            aggregatedDataProfile.relations.filter { it.sourceId == source.id }.forEach { relation ->
                createRelationRoute(aggregatedDataProfile, relation)
                multicast = multicast.to("direct:relation_${relation.id}")
            }

            multicast.end()
        }
    }

    override fun configure() {
        val relations = aggregatedDataProfile.relations.filter { it.sourceId == null }

        val connectorInstance = connectorInstanceRepository.findById(aggregatedDataProfile.connectorInstanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connectorEndpoint = connectorEndpointRepository.findById(aggregatedDataProfile.connectorEndpointId)
            .orElseThrow { NoSuchElementException("Connector endpoint not found") }

        val effectiveRole = aggregatedDataProfile.role?.takeIf { it.isNotBlank() } ?: run {
            val sanitizedName = aggregatedDataProfile.name.replace(Regex("[^0-9a-zA-Z_-]+"), "")
            "ROLE_AGGREGATED_DATA_PROFILE_${sanitizedName.uppercase()}"
        }

        onException(AccessDeniedException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.UNAUTHORIZED.value()))

        from("direct:aggregated_data_profile_${aggregatedDataProfile.id}")
            .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_direct")
            .streamCache(true)
            .setVariable(
                "authorities",
                constant(effectiveRole)
            )
            .to("direct:auth")
            .setVariable("connector", constant(connectorInstance.connector.tag))
            .setVariable("config", constant(connectorInstance.tag))
            .setVariable("operation", constant(connectorEndpoint.operation))
            .process(adpCacheCheckProcessor) // When cache hits it stops route
            .to(Iko.endpoint("validate"))
            .to(Iko.iko("config"))
            .to(Iko.transform())
            .to(Iko.connector())
            .let {
                if (relations.isNotEmpty()) {
                    it.enrich("direct:multicast_${aggregatedDataProfile.id}", PairAggregator)
                } else {
                    it
                }
            }
            .transform(jq(aggregatedDataProfile.transform.expression))
            .marshal().json()
            .process(adpCachePutProcessor)
        if (relations.isNotEmpty()) {
            var multicast = from("direct:multicast_${aggregatedDataProfile.id}")
                .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")
                .multicast(MapAggregator)
                .parallelProcessing()

            aggregatedDataProfile.relations.filter { it.sourceId == null }.forEach { relation ->
                createRelationRoute(aggregatedDataProfile, relation)
                multicast = multicast.to("direct:relation_${relation.id}")
            }
            multicast.end()
        }
    }

}