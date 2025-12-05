package com.ritense.iko.aggregateddataprofile.camel

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.cache.domain.toCacheable
import com.ritense.iko.cache.processor.CacheProcessor
import com.ritense.iko.connectors.camel.Iko
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.builder.FlexibleAggregationStrategy
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jackson.JacksonConstants
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException

class AggregatedDataProfileRouteBuilder(
    private val camelContext: CamelContext,
    private val aggregatedDataProfile: AggregatedDataProfile,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val cacheProcessor: CacheProcessor,
) : RouteBuilder(camelContext) {

    override fun configure() {
        // prepare relation variables
        val relations = aggregatedDataProfile.relations.filter { it.sourceId == null }

        val connectorInstance = requireNotNull(
            connectorInstanceRepository.findByIdOrNull(aggregatedDataProfile.connectorInstanceId)
        ) { NoSuchElementException("Connector instance not found") }
        val connectorEndpoint = requireNotNull(
            connectorEndpointRepository.findByIdOrNull(aggregatedDataProfile.connectorEndpointId)
        ) { NoSuchElementException("Connector endpoint not found") }

        val effectiveRole = aggregatedDataProfile.role?.takeIf { it.isNotBlank() } ?: run {
            val sanitizedName = aggregatedDataProfile.name.replace(Regex("[^0-9a-zA-Z_-]+"), "")
            "ROLE_AGGREGATED_DATA_PROFILE_${sanitizedName.uppercase()}"
        }

        onException(AccessDeniedException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.UNAUTHORIZED.value()))

        // profile root route entrypoint
        from("direct:aggregated_data_profile_${aggregatedDataProfile.id}")
            .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_root")
            .routeDescription("ADP root route")
            .setVariable(
                "authorities",
                constant(effectiveRole),
            )
            .to("direct:auth")
            .setVariable("connector", constant(connectorInstance.connector.tag))
            .setVariable("config", constant(connectorInstance.tag))
            .setVariable("operation", constant(connectorEndpoint.operation))
            .to(Iko.endpoint("validate"))
            .to(Iko.iko("config"))
            .to(Iko.transform())
            .process {
                cacheProcessor.checkCache(exchange = it, cacheable = aggregatedDataProfile.toCacheable())
            }
            .to(Iko.connector())
            .let {
                if (relations.isNotEmpty()) {
                    // enrich root exchange with multicast routes from relations
                    it.enrich("direct:multicast_${aggregatedDataProfile.id}", PairAggregator)
                } else {
                    it
                }
            }
            // transform adp result
            .transform(jq(aggregatedDataProfile.transform.expression))
            .process {
                // put final value into cache if relevant
                cacheProcessor.putCache(exchange = it, cacheable = aggregatedDataProfile.toCacheable())
            }
            .marshal().json()

        // create multicast and build routes for relations
        if (relations.isNotEmpty()) {
            var multicast = from("direct:multicast_${aggregatedDataProfile.id}")
                .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")
                .multicast(MapAggregator)
                .parallelProcessing()

            relations.forEach { relation ->
                // build route for child relation
                buildRelationRoute(aggregatedDataProfile, relation)
                // invoke relation route as multicast
                multicast = multicast.to("direct:relation_${relation.id}")
            }

            multicast.end()
        }
    }

    private fun buildRelationRoute(rootProfile: AggregatedDataProfile, currentRelation: Relation) {
        camelContext.globalOptions[JacksonConstants.ENABLE_TYPE_CONVERTER] = "true"

        val relations = rootProfile.relations.filter { it.sourceId == currentRelation.id }
        val connectorInstance = connectorInstanceRepository.findById(currentRelation.connectorInstanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connectorEndpoint = connectorEndpointRepository.findById(currentRelation.connectorEndpointId)
            .orElseThrow { NoSuchElementException("Connector endpoint not found") }

        from("direct:relation_${currentRelation.id}")
            .routeId("relation_${currentRelation.id}_root")
            .routeDescription("[${aggregatedDataProfile.name}] <-- [${currentRelation.propertyName}]")
            .removeHeaders("*")
            .setVariable("endpointMapping").jq(currentRelation.sourceToEndpointMapping)
            .setVariable("relationId", constant(currentRelation.id))
            .marshal().json()
            .choice()
            .`when` { ex -> ex.getVariable("endpointMapping", JsonNode::class.java).isArray }
            .to("direct:relation_${currentRelation.id}_array")
            .`when` { ex -> ex.getVariable("endpointMapping", JsonNode::class.java).isObject }
            .to("direct:relation_${currentRelation.id}_map")

        from("direct:relation_${currentRelation.id}_map")
            .routeId("relation_${currentRelation.id}_map")
            .routeDescription("Endpoint mapping (Map): [${currentRelation.propertyName}]")
            .process {
                it.getVariable("endpointMapping", ObjectNode::class.java).forEachEntry { key, value ->
                    it.getIn().setHeader(key, value.asText())
                }
            }
            .removeVariable("endpointMapping")
            .to("direct:relation_${currentRelation.id}_loop")
            .transform(jq(currentRelation.transform.expression))
            .unmarshal().json()

        from("direct:relation_${currentRelation.id}_array")
            .routeId("relation_${currentRelation.id}_array")
            .routeDescription("Endpoint mapping (List): [${currentRelation.propertyName}]")
            .split(
                variable("endpointMapping"),
                FlexibleAggregationStrategy<JsonNode>()
                    .pick(body())
                    .castAs(JsonNode::class.java)
                    .accumulateInCollection(ArrayList::class.java),
            )
            .parallelProcessing()
            .process { ex ->
                ex.getIn().getBody(ObjectNode::class.java).forEachEntry { key, value ->
                    ex.getIn().setHeader(key, value.asText())
                }
            }
            .removeVariable("endpointMapping")
            .to("direct:relation_${currentRelation.id}_loop") // Executes the relation route
            .end()
            .transform(jq(currentRelation.transform.expression))
            .unmarshal().json()

        // Executes each relation route
        from("direct:relation_${currentRelation.id}_loop")
            .routeId("relation_${currentRelation.id}_loop")
            .routeDescription("[${currentRelation.propertyName}] --> Endpoint")
            .unmarshal().json()
            .setVariable("connector", constant(connectorInstance.connector.tag))
            .setVariable("config", constant(connectorInstance.tag))
            .setVariable("operation", constant(connectorEndpoint.operation))
            .setVariable("relationPropertyName", constant(currentRelation.propertyName))
            .to(Iko.endpoint("validate"))
            .to(Iko.iko("config"))
            .to(Iko.transform())
            .process {
                cacheProcessor.checkCache(exchange = it, cacheable = currentRelation.toCacheable())
            }
            .choice()
            .`when` { ex -> !ex.getVariable("cacheHit_${currentRelation.id}", Boolean::class.java) } // cacheHit false
            .to(Iko.connector())
            .process {
                cacheProcessor.putCache(exchange = it, cacheable = currentRelation.toCacheable())
            }
            .end()
            .let {
                if (relations.isNotEmpty()) {
                    it.enrich("direct:multicast_${currentRelation.id}", PairAggregator)
                } else {
                    it
                }
            }

        if (relations.isNotEmpty()) {
            // create new multicast processor
            var multicast = from("direct:multicast_${currentRelation.id}")
                .routeId("relation_${currentRelation.id}_multicast")
                .multicast(MapAggregator)
                .parallelProcessing()

            relations.forEach { relation ->
                // build route for child relation
                buildRelationRoute(rootProfile, relation)
                // invoke relation route as multicast
                multicast = multicast.to("direct:relation_${relation.id}")
            }

            multicast.end()
        }
    }
}