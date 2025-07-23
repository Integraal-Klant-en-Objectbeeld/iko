package com.ritense.iko.profile

import com.ritense.iko.endpoints.EndpointRepository
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException

class ProfileRouteBuilder(
    private val camelContext: CamelContext,
    private val profile: Profile,
    private val endpointRepository: EndpointRepository
) : RouteBuilder(camelContext) {

    fun createRelationRoute(profile: Profile, source: Relation) {
        val relations = profile.relations.filter { it.sourceId == source.id }
        val searchDirectName = endpointRepository.getReferenceById(UUID.fromString(source.searchId)).routeId // TODO FIX table col type
        from("direct:relation_${source.id}")
            .routeId("relation_${source.id}_direct")
            .removeHeaders("*")
            .marshal().json()
            .let {
                var y = it
                source.sourceToSearchMappingAsMap().forEach { entry ->
                    y = y.setHeader(entry.key).jq(entry.value, String::class.java)
                }
                y
            }
            .unmarshal().json()
            .to("direct:${searchDirectName}").let {
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

            profile.relations.filter { it.sourceId == source.id }.forEach { relation ->
                createRelationRoute(profile, relation)

                multicast = multicast.to("direct:relation_${relation.id}")
            }

            multicast.end()
        }
    }

    override fun configure() {
        val relations = profile.relations.filter { it.sourceId == null }
        val searchDirectName = endpointRepository.getReferenceById(profile.primarySearch).routeId

        onException(AccessDeniedException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.UNAUTHORIZED.value()))

        from("direct:profile_${profile.id}")
            .routeId("profile_${profile.id}_direct")
            // TODO: Replace this constant with a ROLE that you can set on the profile.
            .setVariable("authorities", constant("ROLE_PROFILE_${profile.name.replace("[^0-9a-zA-Z_\\-]+", "").uppercase()}"))
            .to("direct:auth")
            .to("direct:$searchDirectName")  // TODO this needs to be looked up in the DB is this is enabled/exists there as well.
            .let {
                if (relations.isNotEmpty()) {
                    it.enrich("direct:multicast_${profile.id}", PairAggregator)
                } else {
                    it
                }
            }
            .transform(jq(profile.transform.expression))
            .marshal().json()

        if (relations.isNotEmpty()) {
            var multicast = from("direct:multicast_${profile.id}")
                .routeId("profile_${profile.id}_multicast")
                .multicast(MapAggregator)
                .parallelProcessing()

            profile.relations.filter { it.sourceId == null }.forEach { relation ->
                createRelationRoute(profile, relation)

                multicast = multicast.to("direct:relation_${relation.id}")
            }

            multicast.end()
        }
    }
}