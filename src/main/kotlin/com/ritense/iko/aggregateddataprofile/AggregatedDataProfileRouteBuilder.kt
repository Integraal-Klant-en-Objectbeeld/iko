package com.ritense.iko.aggregateddataprofile

import com.ritense.iko.endpoints.EndpointRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import java.util.UUID

class AggregatedDataProfileRouteBuilder(
    private val camelContext: CamelContext,
    private val aggregatedDataProfile: AggregatedDataProfile,
    private val endpointRepository: EndpointRepository
) : RouteBuilder(camelContext) {

    fun createRelationRoute(aggregatedDataProfile: AggregatedDataProfile, source: Relation) {
        val relations = aggregatedDataProfile.relations.filter { it.sourceId == source.id }
        val routeId = endpointRepository.getReferenceById(UUID.fromString(source.endpointId)).routeId // TODO FIX table col type
        from("direct:relation_${source.id}")
            .routeId("relation_${source.id}_direct")
            .removeHeaders("*")
            .marshal().json()
            .let {
                var y = it
                source.sourceToEndpointMappingAsMap().forEach { entry ->
                    y = y.setHeader(entry.key).jq(entry.value, String::class.java)
                }
                y
            }
            .unmarshal().json()
            .to("direct:${routeId}").let {
                if (relations.isNotEmpty()) {
                    it.enrich("direct:multicast_${source.id}", PairAggregator)
                } else {
                    it
                }
            }
            .transform(jq(source.transform.expression))
            .unmarshal().json()

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
        val endpointRoute = endpointRepository.getReferenceById(aggregatedDataProfile.primaryEndpoint)

        if(!endpointRoute.isPrimary) {
            logger.warn { "Skipping configure: The endpoint ${aggregatedDataProfile.primaryEndpoint} is not primary" }
            return
        }
        if(!endpointRoute.isActive) {
            logger.warn { "Skipping configure: The endpoint ${aggregatedDataProfile.primaryEndpoint} is not active" }
            return
        }

        onException(AccessDeniedException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.UNAUTHORIZED.value()))

        from("direct:aggregated_data_profile_${aggregatedDataProfile.id}")
            .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_direct")
            // TODO: Replace this constant with a ROLE that you can set on the profile.
            .setVariable(
                "authorities",
                constant("ROLE_PROFILE_${aggregatedDataProfile.name.replace("[^0-9a-zA-Z_\\-]+", "").uppercase()}")
            )
            .to("direct:auth")
            .to("direct:${endpointRoute.routeId}")
            .let {
                if (relations.isNotEmpty()) {
                    it.enrich("direct:multicast_${aggregatedDataProfile.id}", PairAggregator)
                } else {
                    it
                }
            }
            .transform(jq(aggregatedDataProfile.transform.expression))
            .marshal().json()

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

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}