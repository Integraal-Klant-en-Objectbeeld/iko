package com.ritense.iko.aggregateddataprofile.camel

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.ENDPOINT_TRANSFORM_CONTEXT_VARIABLE
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.ENDPOINT_TRANSFORM_RESULT_VARIABLE
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileUnsupportedEndpointTransformResultTypeError
import com.ritense.iko.aggregateddataprofile.error.errorResponse
import com.ritense.iko.cache.domain.toCacheable
import com.ritense.iko.cache.processor.CacheProcessor
import com.ritense.iko.connectors.camel.Iko
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import org.apache.camel.CamelContext
import org.apache.camel.ValidationException
import org.apache.camel.builder.FlexibleAggregationStrategy
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jackson.JacksonConstants
import org.apache.camel.http.base.HttpOperationFailedException
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
        camelContext.globalOptions[JacksonConstants.ENABLE_TYPE_CONVERTER] = "true"

        // prepare relation variables
        val level1Relations = aggregatedDataProfile.level1Relations()

        val connectorInstance = requireNotNull(
            connectorInstanceRepository.findByIdOrNull(aggregatedDataProfile.connectorInstanceId),
        ) { NoSuchElementException("Connector instance not found") }
        val connectorEndpoint = requireNotNull(
            connectorEndpointRepository.findByIdOrNull(aggregatedDataProfile.connectorEndpointId),
        ) { NoSuchElementException("Connector endpoint not found") }

        // Error section
        onException(AccessDeniedException::class.java)
            .errorResponse(status = HttpStatus.UNAUTHORIZED, exposeMessage = false)

        onException(ValidationException::class.java, IllegalArgumentException::class.java)
            .errorResponse(status = HttpStatus.BAD_REQUEST)

        onException(HttpOperationFailedException::class.java)
            .errorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                exposeMessage = false,
            )

        onException(AggregatedDataProfileUnsupportedEndpointTransformResultTypeError::class.java)
            .errorResponse(
                status = HttpStatus.BAD_REQUEST,
                exposeMessage = true,
            )

        // Global
        onException(Exception::class.java)
            .errorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                exposeMessage = false,
            )

        val endpointTransformExpression = expression()
            .jq(aggregatedDataProfile.endpointTransform.expression)
            .variableName(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE)
            .resultType(JsonNode::class.java)
            .end()

        // profile root route entrypoint
        from("direct:aggregated_data_profile_${aggregatedDataProfile.id}")
            .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_root")
            .routeDescription("[ADP Root]")
            .setVariable(
                "authorities",
                constant(aggregatedDataProfile.roles.asList()),
            )
            .to("direct:auth")
            .to("direct:aggregated_data_profile_${aggregatedDataProfile.id}_endpoint_transform")
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
                if (level1Relations.isNotEmpty()) {
                    // enrich root exchange with multicast routes from relations
                    it.enrich("direct:multicast_${aggregatedDataProfile.id}", PairAggregator)
                } else {
                    it
                }
            }
            // transform adp result
            .transform(jq(aggregatedDataProfile.resultTransform.expression))
            .process {
                // put final value into cache if relevant
                cacheProcessor.putCache(exchange = it, cacheable = aggregatedDataProfile.toCacheable())
            }
            .marshal().json()

        from("direct:aggregated_data_profile_${aggregatedDataProfile.id}_endpoint_transform")
            .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_endpoint_transform")
            .setVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, endpointTransformExpression)
            .routeDescription("[ADP Endpoint Transform]")
            .process { exchange ->
                val endpointTransformResult: JsonNode? =
                    exchange.getVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, JsonNode::class.java)

                when (endpointTransformResult) {
                    is ObjectNode -> endpointTransformResult.forEachEntry { key, value ->
                        exchange.getIn().setHeader(key, value.asText())
                    }
                    null -> return@process
                    else -> throw AggregatedDataProfileUnsupportedEndpointTransformResultTypeError(
                        type = endpointTransformResult::class.java.simpleName,
                    )
                }
            }

        // create multicast and build routes for relations
        if (level1Relations.isNotEmpty()) {
            var multicast = from("direct:multicast_${aggregatedDataProfile.id}")
                .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")
                .multicast(MapAggregator)
                .parallelProcessing()

            level1Relations.forEach { relation ->
                // build route for child relation
                buildRelationRoute(relation)
                // invoke relation route as multicast
                multicast = multicast.to("direct:relation_${relation.id}")
            }

            multicast.end()
        }
    }

    private fun buildRelationRoute(currentRelation: Relation) {
        val relations = currentRelation.aggregatedDataProfile.relationsOf(currentRelation.id)
        val connectorInstance = connectorInstanceRepository.findById(currentRelation.connectorInstanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connectorEndpoint = connectorEndpointRepository.findById(currentRelation.connectorEndpointId)
            .orElseThrow { NoSuchElementException("Connector endpoint not found") }

        val sourceToEndpointTransformExpression = expression()
            .jq(currentRelation.endpointTransform.expression)
            .variableName(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE)
            .resultType(JsonNode::class.java)
            .end()

        from("direct:relation_${currentRelation.id}")
            .routeId("relation_${currentRelation.id}_root")
            .routeDescription("[${aggregatedDataProfile.name}] <-- [${currentRelation.propertyName}]")
            .removeHeaders("*")
            .removeVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE)
            .process { exchange ->
                val endpointTransformContext =
                    exchange.getVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, ObjectNode::class.java)
                val body = exchange.message.getBody(JsonNode::class.java)
                val updatedContext = endpointTransformContext.set<JsonNode>("source", body)
                exchange.setVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, updatedContext)
            }
            .setVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, sourceToEndpointTransformExpression)
            .setVariable("relationId", constant(currentRelation.id))
            .marshal().json()
            .choice()
            .`when` { ex -> ex.getVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, JsonNode::class.java).isArray }
            .to("direct:relation_${currentRelation.id}_array")
            .`when` { ex -> ex.getVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, JsonNode::class.java).isObject }
            .to("direct:relation_${currentRelation.id}_map")

        from("direct:relation_${currentRelation.id}_map")
            .routeId("relation_${currentRelation.id}_map")
            .routeDescription("Endpoint mapping (Map): [${currentRelation.propertyName}]")
            .process {
                it.getVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE, ObjectNode::class.java).forEachEntry { key, value ->
                    it.getIn().setHeader(key, value.asText())
                }
            }
            .to("direct:relation_${currentRelation.id}_loop")
            .transform(jq(currentRelation.resultTransform.expression))
            .unmarshal().json()

        from("direct:relation_${currentRelation.id}_array")
            .routeId("relation_${currentRelation.id}_array")
            .routeDescription("Endpoint mapping (List): [${currentRelation.propertyName}]")
            .split(
                variable(ENDPOINT_TRANSFORM_RESULT_VARIABLE),
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
            .to("direct:relation_${currentRelation.id}_loop") // Executes the relation route
            .end()
            .transform(jq(currentRelation.resultTransform.expression))
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
            .removeVariable(ENDPOINT_TRANSFORM_RESULT_VARIABLE)
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
                buildRelationRoute(relation)
                // invoke relation route as multicast
                multicast = multicast.to("direct:relation_${relation.id}")
            }

            multicast.end()
        }
    }
}