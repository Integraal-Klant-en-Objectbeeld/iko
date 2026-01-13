package com.ritense.iko.mvc.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_ID_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.ADP_PROFILE_NAME_PARAM_HEADER
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Headers.IKO_TRACE_ID_HEADER
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
        tracer.traceFilter = "\${header.$IKO_TRACE_ID_HEADER} == '$ikoTraceId'"
        tracer.clear() // Clean history first

        // Run ADP
        val adpEndpointUri = "direct:aggregated_data_profile_rest_continuation"
        val headers =
            mapOf(
                ADP_ID_PARAM_HEADER to form.testId,
                ADP_PROFILE_NAME_PARAM_HEADER to form.name,
                IKO_TRACE_ID_HEADER to ikoTraceId,
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
            addObject("testId", form.testId)
            addObject("endpointTransformContext", form.endpointTransformContext)
            addObject("testResult", result)
            addObject("traces", traces)
            addObject("exception", exception)
        }
    }
}