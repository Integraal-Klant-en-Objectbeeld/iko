package com.ritense.iko.aggregateddataprofile.camel

import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import org.apache.camel.builder.RouteBuilder

class AggregatedDataProfileRoute(
    val aggregatedDataProfileRepository: AggregatedDataProfileRepository
) : RouteBuilder() {
    override fun configure() {
        rest("/aggregated-data-profiles")
            .get("/{iko_profile}/{iko_id}")
            .to("direct:aggregated_data_profile_rest")

        from("direct:aggregated_data_profile_rest")
            .routeId("aggregated-data-profile-rest")
            .to("direct:aggregated_data_profile_rest_continuation")

        from("direct:aggregated_data_profile_rest_continuation")
            .routeId("aggregated-data-profile-rest-continuation")
            .setVariable("id", header("iko_id"))
            .setVariable("profile", header("iko_profile"))
            .removeHeaders("iko_*", "iko_trace_id")
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