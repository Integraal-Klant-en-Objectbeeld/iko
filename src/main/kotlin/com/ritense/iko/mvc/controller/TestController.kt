/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.mvc.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.aggregateddataprofile.service.AggregatedDataProfileService
import com.ritense.iko.camel.IkoConstants.Headers.ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER
import com.ritense.iko.camel.IkoConstants.Headers.ADP_PROFILE_NAME_PARAM_HEADER
import com.ritense.iko.camel.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_ADP
import com.ritense.iko.mvc.model.ExceptionResponse
import com.ritense.iko.mvc.model.TestAggregatedDataProfileForm
import com.ritense.iko.mvc.model.TraceEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.apache.camel.CamelContext
import org.apache.camel.CamelExecutionException
import org.apache.camel.ProducerTemplate
import org.apache.camel.spi.BacklogTracer
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import java.util.UUID

@Controller
@RequestMapping("/admin")
internal class TestController(
    private val producerTemplate: ProducerTemplate,
    private val camelContext: CamelContext,
    private val objectMapper: ObjectMapper,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val aggregatedDataProfileService: AggregatedDataProfileService,
) {
    private val logger = KotlinLogging.logger {}

    @PostMapping(
        path = ["/aggregated-data-profiles/debug"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    fun test(
        @Valid @ModelAttribute form: TestAggregatedDataProfileForm,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        // Lookup the ADP by name and version
        val aggregatedDataProfile = aggregatedDataProfileRepository
            .findByNameAndVersion(form.name, form.version)
            ?: throw NoSuchElementException("AggregatedDataProfile not found: ${form.name} v${form.version}")

        // For non-active versions, we need to ensure routes are available for testing
        val needsTemporaryRoutes = !aggregatedDataProfile.isActive
        if (needsTemporaryRoutes) {
            ensureRoutesAvailable(aggregatedDataProfile)
        }

        try {
            return executeTest(form, aggregatedDataProfile)
        } finally {
            // Suspend routes after testing if they were loaded temporarily
            if (needsTemporaryRoutes) {
                suspendRoutes(aggregatedDataProfile)
            }
        }
    }

    private fun executeTest(
        form: TestAggregatedDataProfileForm,
        aggregatedDataProfile: AggregatedDataProfile,
    ): ModelAndView {
        val tracer = camelContext.camelContextExtension.getContextPlugin(BacklogTracer::class.java)
        requireNotNull(tracer) { "BacklogTracer plugin not found in CamelContext" }
        val ikoTraceId = UUID.randomUUID().toString()
        tracer.isEnabled = true
        tracer.traceFilter = "\${variable.$IKO_TRACE_ID_VARIABLE} == '$ikoTraceId'"
        tracer.clear() // Clean history first

        // Run ADP - use the specific ADP's route, not the name-based lookup
        val adpEndpointUri = "direct:aggregated_data_profile_${aggregatedDataProfile.id}"
        val headers =
            mapOf(
                ADP_PROFILE_NAME_PARAM_HEADER to form.name,
                IKO_TRACE_ID_VARIABLE to ikoTraceId,
                ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER to objectMapper.readTree(form.endpointTransformContext),
            )
        var result: String? = null
        var exception: ExceptionResponse? = null
        try {
            val adpResult =
                producerTemplate.requestBodyAndHeaders(
                    adpEndpointUri,
                    "{}",
                    headers,
                    String::class.java,
                )
            val jsonResult = objectMapper.readTree(adpResult)
            result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResult)
        } catch (ex: CamelExecutionException) {
            exception = ExceptionResponse.of(ex)
        }

        // Fetch traces
        val traces = tracer.dumpAllTracedMessages()?.mapNotNull {
            it?.let { TraceEvent.from(it) }
        } ?: emptyList()

        // Disable tracing
        tracer.isEnabled = false

        return ModelAndView("$BASE_FRAGMENT_ADP/debug :: profile-debug").apply {
            addObject("form", form)
            addObject("endpointTransformContext", form.endpointTransformContext)
            addObject("testResult", result)
            addObject("traces", traces)
            addObject("exception", exception)
        }
    }

    /**
     * Ensures routes are available for testing.
     * - If routes don't exist: register them
     * - If routes exist but are stopped: resume them
     * - If routes are already running: do nothing
     */
    private fun ensureRoutesAvailable(aggregatedDataProfile: AggregatedDataProfile) {
        val routeId = "aggregated_data_profile_${aggregatedDataProfile.id}_root"
        val existingRoute = camelContext.getRoute(routeId)

        if (existingRoute == null) {
            // Routes not registered yet - add them
            logger.debug { "Adding routes for non-active ADP: ${aggregatedDataProfile.name} v${aggregatedDataProfile.version}" }
            aggregatedDataProfileService.addRoutes(aggregatedDataProfile)
        } else {
            // Routes exist - check if they need to be resumed
            val routeController = camelContext.routeController
            val status = routeController.getRouteStatus(routeId)
            if (status.isStopped || status.isSuspended) {
                // Resume all routes in the ADP's group
                logger.debug { "Resuming routes for ADP: ${aggregatedDataProfile.name} v${aggregatedDataProfile.version}" }
                val groupName = "adp_${aggregatedDataProfile.id}"
                camelContext.getRoutesByGroup(groupName).forEach { route ->
                    routeController.resumeRoute(route.id)
                }
            }
        }
    }

    /**
     * Suspends (stops) routes after testing.
     * Routes remain registered but inactive, avoiding re-registration overhead on next test.
     */
    private fun suspendRoutes(aggregatedDataProfile: AggregatedDataProfile) {
        val groupName = "adp_${aggregatedDataProfile.id}"
        val routeController = camelContext.routeController
        logger.debug { "Suspending routes for ADP: ${aggregatedDataProfile.name} v${aggregatedDataProfile.version}" }
        camelContext.getRoutesByGroup(groupName).forEach { route ->
            routeController.suspendRoute(route.id)
        }
    }
}