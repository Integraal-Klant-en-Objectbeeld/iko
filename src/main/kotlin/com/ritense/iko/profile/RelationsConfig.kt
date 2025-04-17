package com.ritense.iko.profile

import com.ritense.iko.aggregator.ResponseAggregator
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.springframework.context.annotation.Configuration
import java.util.UUID

@Configuration
class RelationsConfig(private val camelContext: CamelContext) {

    var profileId = UUID.randomUUID()
    var relationId = UUID.randomUUID()
    var profiles: List<Profile> = listOf(
        Profile(
            id = profileId,
            name = "profile1",
            primarySource = "personenSearch",
            relations = listOf(
                Relation(
                    id = UUID.randomUUID(),
                    profileId = profileId,
                    sourceToSearchMapping = "{ \"bsn\": \".burgerservicenummer\" }",
                    searchId = "zakenSearch_bsn",
                    transform = "{ zaken1: . }"
                ),
                Relation(
                    id = relationId,
                    profileId = profileId,
                    sourceToSearchMapping = "{ \"bsn\": \".burgerservicenummer\" }",
                    searchId = "zakenSearch_bsn",
                    transform = "{ zaken2: [ .primary.results[], .secondary.zaken3 ] }"
                ),
                Relation(
                    id = UUID.randomUUID(),
                    profileId = profileId,
                    sourceId = relationId,
                    sourceToSearchMapping = "{ \"id\": \".results[0].uuid\" }",
                    searchId = "zakenSearch",
                    transform = "{ zaken3: . }"
                )
            ),
            transform = "{ persoon: .primary, zaken: [ .secondary.zaken1.results[], .secondary.zaken2[] ] }"
        )
    )

    init {
        this.profiles.forEach { profile ->
            camelContext.addRoutes(ProfileRouteBuilder(camelContext, profile))
        }
    }

}

class ProfileRouteBuilder(private val camelContext: CamelContext, private val profile: Profile) :
    RouteBuilder(camelContext) {

    fun createRelationRoute(profile: Profile, source: Relation) {
        val relations = profile.relations.filter { it.sourceId == source.id }

        from("direct:relation_${source.id}")
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
            .to("direct:${source.searchId}").let {
                if (relations.isNotEmpty()) {
                    it.enrich("direct:multicast_${source.id}", ProfileResponseAggregator)
                } else {
                    it
                }
            }
            .transform(jq(source.transform))
            .unmarshal().json()

        if (relations.isNotEmpty()) {
            var multicast = from("direct:multicast_${source.id}")
                .multicast(ResponseAggregator)
                .parallelProcessing()

            profile.relations.filter { it.sourceId == source.id }.forEach { relation ->
                createRelationRoute(profile, relation)

                multicast = multicast.to("direct:relation_${relation.id}")
            }

            multicast.end()
        }
    }

    override fun configure() {
        rest("/profile/${profile.name}")
            .get("/{id}")
            .routeId("profile_${profile.id}")
            .to("direct:profile_${profile.id}")

        val relations = profile.relations.filter { it.sourceId == null }

        from("direct:profile_${profile.id}")
            .to("direct:${profile.primarySource}")
            .let {
                if (relations.isNotEmpty()) {
                    it.enrich("direct:multicast_${profile.id}", ProfileResponseAggregator)
                } else {
                    it
                }
            }
            .transform(jq(profile.transform))
            .marshal().json()

        if (relations.isNotEmpty()) {
            var multicast = from("direct:multicast_${profile.id}")
                .multicast(ResponseAggregator)
                .parallelProcessing()

            profile.relations.filter { it.sourceId == null }.forEach { relation ->
                createRelationRoute(profile, relation)

                multicast = multicast.to("direct:relation_${relation.id}")
            }

            multicast.end()
        }
    }
}