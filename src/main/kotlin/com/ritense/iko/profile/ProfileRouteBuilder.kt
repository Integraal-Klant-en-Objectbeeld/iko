package com.ritense.iko.profile

import com.ritense.iko.search.SearchRepository
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import java.util.UUID

class ProfileRouteBuilder(
    private val camelContext: CamelContext,
    private val profile: Profile,
    private val searchRepository: SearchRepository
) : RouteBuilder(camelContext) {

    fun createRelationRoute(profile: Profile, source: Relation) {
        val relations = profile.relations.filter { it.sourceId == source.id }
        val searchDirectName = searchRepository.getReferenceById(UUID.fromString(source.searchId)).routeId // TODO FIX table col type
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
        val searchDirectName = searchRepository.getReferenceById(profile.primarySearch).routeId
        from("direct:profile_${profile.id}")
            .routeId("profile_${profile.id}_direct")
            .to("direct:$searchDirectName")
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