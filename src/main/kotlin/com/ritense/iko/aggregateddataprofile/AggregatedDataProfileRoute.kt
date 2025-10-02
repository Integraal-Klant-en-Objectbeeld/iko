package com.ritense.iko.aggregateddataprofile

import org.apache.camel.builder.RouteBuilder

class AggregatedDataProfileRoute(
    val aggregatedDataProfileRepository: AggregatedDataProfileRepository
) : RouteBuilder() {
    override fun configure() {
        rest("/aggregated-data-profiles")
            .get("/{iko_profile}/{iko_id}")
            .to("direct:aggregated_data_profile_rest")

        from("direct:aggregated_data_profile_rest")
            .routeId("aggregated_data_profile_rest")
            .setVariable("id", header("iko_id"))
            .setVariable("profile", header("iko_profile"))
            .removeHeaders("iko_*")
            .process { exchange ->
                val aggregatedDataProfileName = exchange.getVariable("profile", String::class.java)
                val aggregatedDataProfile = aggregatedDataProfileRepository.findByName(aggregatedDataProfileName)
                if (aggregatedDataProfile == null) {
                    throw IllegalArgumentException("AggregatedDataProfile with name '$aggregatedDataProfileName' not found")
                }

                exchange.setVariable("aggregatedDataProfile", aggregatedDataProfile)
                exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile!!.id)
            }
            .toD("direct:aggregated_data_profile_\${variable.aggregatedDataProfileId}")
    }
}