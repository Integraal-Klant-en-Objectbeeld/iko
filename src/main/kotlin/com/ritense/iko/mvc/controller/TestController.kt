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
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_PROFILE_NAME_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_ADP
import com.ritense.iko.mvc.model.ExceptionResponse
import com.ritense.iko.mvc.model.TestAggregatedDataProfileForm
import com.ritense.iko.mvc.model.TraceEvent
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
class TestController(
    private val producerTemplate: ProducerTemplate,
    private val camelContext: CamelContext,
    private val objectMapper: ObjectMapper,
) {
    @PostMapping(
        path = ["/aggregated-data-profiles/debug"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    fun test(
        @Valid @ModelAttribute form: TestAggregatedDataProfileForm,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        val tracer = camelContext.camelContextExtension.getContextPlugin(BacklogTracer::class.java)
        requireNotNull(tracer) { "BacklogTracer plugin not found in CamelContext" }
        val ikoTraceId = UUID.randomUUID().toString()
        tracer.isEnabled = true
        tracer.traceFilter = "\${variable.$IKO_TRACE_ID_VARIABLE} == '$ikoTraceId'"
        tracer.clear() // Clean history first

        // Run ADP
        val adpEndpointUri = "direct:aggregated_data_profile_rest_continuation"
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
}