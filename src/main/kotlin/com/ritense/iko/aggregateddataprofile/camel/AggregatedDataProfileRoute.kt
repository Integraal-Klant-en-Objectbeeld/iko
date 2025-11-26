package com.ritense.iko.aggregateddataprofile.camel

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.service.CacheService
import org.apache.camel.builder.RouteBuilder

class AggregatedDataProfileRoute(
    val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    val cacheService: CacheService,
    val objectMapper: ObjectMapper
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
                exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile.id)
                exchange.setVariable("cacheEnabled", aggregatedDataProfile.aggregatedDataProfileCacheSetting.enabled)
                exchange.setVariable("cacheTTL", aggregatedDataProfile.aggregatedDataProfileCacheSetting.timeToLive)

                // TODO change after ADP params are introduced
                // Pass on the objects downstream
                val containerParams = emptyList<ContainerParam>()
                val filterParams = emptyMap<String, String>()
                val adpEndpointParameterMapping = ""

                exchange.setVariable("containerParams", containerParams)
                exchange.setVariable("filterParams", filterParams)
                exchange.setVariable("adpEndpointParameterMapping", adpEndpointParameterMapping)

                if (aggregatedDataProfile.aggregatedDataProfileCacheSetting.enabled) {
                    val combined = objectMapper.writeValueAsString(
                        mapOf(
                            "aggregatedDataProfileId" to aggregatedDataProfile.id,
                            "containerParams" to containerParams,
                            "filterParams" to filterParams,
                            "adpEndpointParameterMapping" to adpEndpointParameterMapping
                        )
                    )
                    val cacheKey = cacheService.hashString(combined)
                    exchange.setVariable("cacheKey", cacheKey)
                }
            }
            .toD("direct:aggregated_data_profile_\${variable.aggregatedDataProfileId}")
    }
}