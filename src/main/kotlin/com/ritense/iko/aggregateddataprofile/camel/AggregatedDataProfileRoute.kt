package com.ritense.iko.aggregateddataprofile.camel

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileNotFound
import com.ritense.iko.aggregateddataprofile.error.errorResponse
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import org.apache.camel.builder.RouteBuilder
import org.springframework.http.HttpStatus

class AggregatedDataProfileRoute(
    val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    val objectMapper: ObjectMapper,
) : RouteBuilder() {
    override fun configure() {
        onException(AggregatedDataProfileNotFound::class.java)
            .errorResponse(status = HttpStatus.NOT_FOUND)

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
            .setVariable("correlationId", simple("\${exchangeId}"))
            .removeHeaders("iko_*", "iko_trace_id")
            .process { exchange ->
                val aggregatedDataProfileName = exchange.getVariable("profile", String::class.java)
                val aggregatedDataProfile = aggregatedDataProfileRepository.findByName(aggregatedDataProfileName)
                if (aggregatedDataProfile == null) {
                    throw AggregatedDataProfileNotFound(aggregatedDataProfileName)
                } else {
                    exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile.id)
                }
            }
            .toD("direct:aggregated_data_profile_\${variable.aggregatedDataProfileId}")
    }
}