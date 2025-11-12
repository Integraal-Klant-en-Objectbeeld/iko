package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_ADG
import com.ritense.iko.mvc.model.ExceptionResponse
import com.ritense.iko.mvc.model.TestAggregatedDataProfileForm
import com.ritense.iko.mvc.model.TraceEvent
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

@Controller
@RequestMapping("/admin")
class TestController(
    private val producerTemplate: ProducerTemplate,
    private val camelContext: CamelContext
) {

    @PostMapping(
        path = ["/aggregated-data-profiles/test"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun test(
        @Valid @ModelAttribute form: TestAggregatedDataProfileForm,
    ): ModelAndView {
        val tracer = camelContext.camelContextExtension.getContextPlugin(BacklogTracer::class.java)
        requireNotNull(tracer) { "BacklogTracer plugin not found in CamelContext" }
        tracer.clear() // Clean history first

        // Run ADP
        val adpEndpointUri = "direct:aggregated_data_profile_rest_continuation"
        val headers = mapOf(
            "iko_id" to form.testId,
            "iko_profile" to form.name,
        )
        var result = ""
        var exception = ExceptionResponse()
        try {
            result = producerTemplate.requestBodyAndHeaders(
                adpEndpointUri,
                "{}",
                headers,
                String::class.java
            )
        } catch (ex: CamelExecutionException) {
            result = ""
            exception = ExceptionResponse.of(ex)
        }

        // Fetch traces
        val traces = tracer.dumpAllTracedMessages()?.map {
            TraceEvent.from(it)
        } ?: emptyList()
        return ModelAndView("$BASE_FRAGMENT_ADG/test").apply {
            addObject("form", form)
            addObject("testResult", result)
            addObject("traces", traces)
            addObject("exception", exception)
        }
    }

}

