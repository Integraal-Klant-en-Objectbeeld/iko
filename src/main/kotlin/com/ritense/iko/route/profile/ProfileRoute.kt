package com.ritense.iko.route.profile

import com.ritense.iko.aggregator.ResponseAggregator
import org.apache.camel.Exchange
import org.apache.camel.Predicate
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.reifier.RouteReifier

data class Profile(
    var name: String,
    var primarySource: String,
    var secondarySources: List<String>,
    var transform: String
)

fun profile(profilePredicate: (profile: Profile) -> Boolean): (Exchange?) -> Boolean {
    return { exchange: Exchange? ->
        exchange?.getVariable("profile", null, Profile::class.java)
            ?.let { profile -> profilePredicate(profile) }
            ?: false
    }
}
class IPredicate(): Predicate {

    override fun matches(exchange: Exchange?) =
        exchange?.getVariable("profile", Profile::class.java)?.let { profile ->
            return true
        } ?: false
}

class Relations: RouteBuilder() {
    override fun configure() {
        from("relation:A_to_B")
            .multicast(ResponseAggregator)
            .parallelProcessing()
            .choice()
            .`when`(profile { profile -> profile.secondarySources.contains("BRP") })
            .to("direct:secondary_BRP")
            .`when`(profile { profile -> profile.secondarySources.contains("Zaken") })
            .to("direct:secondary_Zaken")
            .endChoice()
            .end()
    }
}


class ProfileRoute : RouteBuilder() {
    override fun configure() {
        // Retrieve JQ profiles
        var profiles = listOf(
            Profile(
                "Tom",
                "BRP",
                listOf("BRP"),
                ".brp"
            ),

            Profile(
                "Niels",
                "BRP",
                listOf("Zaken"),
                "[ .brp, .brp2 ]"
            )
        )

        rest("/profiles")
            .get("/{id}")
            .to("direct:resolveProfile")

        var resolve = from("direct:resolveProfile")
            .choice()

        profiles.forEach { profile ->
            resolve = resolve
                .`when`(simple("\${header.id} == '${profile.name}'"))
                .to("direct:profile_${profile.name}")

            from("direct:profile_${profile.name}")
                .setVariable("profile") { profile }
                .to("direct:primary_${profile.primarySource}")
                .marshal().json()
                .transform(jq(profile.transform))
        }
        resolve.endChoice()

        from("direct:primary_BRP")
            .to("direct:haalcentraal")
            .enrich("direct:relations_BRP", ResponseAggregator)

        from("direct:relations_BRP")
            .multicast(ResponseAggregator)
            .parallelProcessing()
            .choice()
            .`when`(profile { profile -> profile.secondarySources.contains("BRP") })
            .to("direct:secondary_BRP")
            .`when`(profile { profile -> profile.secondarySources.contains("Zaken") })
            .to("direct:secondary_Zaken")
            .endChoice()
            .end()

        from("direct:secondary_BRP")
            .to("direct:haalcentraal")
            .marshal().json().transform(jq("{ brp1: . }")).log("\${body}").unmarshal().json()

        from("direct:secondary_Zaken")
            .to("direct:haalcentraal")
            .marshal().json().transform(jq("{ brp2: . }")).log("\${body}").unmarshal().json()

 // ==== OLD








        from("direct:primary_haalcentraal")
            .to("direct:haalcentraal")
            .enrich("direct:multicast", ResponseAggregator)

        from("direct:multicast")
            .multicast(ResponseAggregator)
            .parallelProcessing()
            .to("direct:secondary_haalcentraal_1")
            .to("direct:secondary_haalcentraal_2")
            .end()

        from("direct:secondary_haalcentraal_1")
            .to("direct:haalcentraal")
            .marshal().json().transform(jq("{ brp1: . }")).log("\${body}").unmarshal().json()

        from("direct:secondary_haalcentraal_2")
            .to("direct:haalcentraal")
            .marshal().json().transform(jq("{ brp2: . }")).log("\${body}").unmarshal().json()

        from("direct:secondary_haalcentraal")
            .to("direct:haalcentraal")
            .marshal().json().transform(jq(".")).unmarshal().json()

    }


}