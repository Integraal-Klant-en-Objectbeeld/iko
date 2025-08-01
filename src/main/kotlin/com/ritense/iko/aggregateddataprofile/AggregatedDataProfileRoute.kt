package com.ritense.iko.aggregateddataprofile

import org.apache.camel.builder.RouteBuilder

class AggregatedDataProfileRoute(
    val aggregatedDataProfileRepository: AggregatedDataProfileRepository
) : RouteBuilder() {
    override fun configure() {
        rest("/aggregated-data-profiles")
            .get("/{aggregatedDataProfileName}/{id}")
            .to("direct:aggregated_data_profile_rest")

        from("direct:aggregated_data_profile_rest")
            .routeId("aggregated_data_profile_rest")
            .process { exchange ->
                val aggregatedDataProfileName = exchange.getIn().getHeader("aggregatedDataProfileName") as String
                val aggregatedDataProfile = aggregatedDataProfileRepository.findByName(aggregatedDataProfileName)
                if (aggregatedDataProfile == null) {
                    throw IllegalArgumentException("AggregatedDataProfile with name '$aggregatedDataProfileName' not found")
                }

                exchange.setVariable("id", exchange.getIn().getHeader("id"))
                exchange.setVariable("aggregatedDataProfile", aggregatedDataProfile)
                exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile!!.id)
            }
            .toD("direct:aggregated_data_profile_\${variable.aggregatedDataProfileId}")
    }
}