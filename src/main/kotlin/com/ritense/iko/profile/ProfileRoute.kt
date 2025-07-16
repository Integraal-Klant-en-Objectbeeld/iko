package com.ritense.iko.profile

import org.apache.camel.builder.RouteBuilder

class ProfileRoute(val profileRepository: ProfileRepository) : RouteBuilder() {
    override fun configure() {
        rest("/profiles")
            .get("/{profileName}/{id}")
            .to("direct:profile_rest")

        from("direct:profile_rest")
            .routeId("profile_rest")
            .process { exchange ->
                val profile = profileRepository.findByName(exchange.getIn().getHeader("profileName") as String)

                exchange.setVariable("id", exchange.getIn().getHeader("id"))
                exchange.setVariable("profile", profile)
                exchange.setVariable("profileId", profile.id)
            }
            .toD("direct:profile_\${variable.profileId}")
    }
}