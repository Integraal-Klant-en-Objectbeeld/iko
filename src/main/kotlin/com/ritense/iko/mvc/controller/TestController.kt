package com.ritense.iko.mvc.controller

import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_ADG
import com.ritense.iko.mvc.model.AggregatedDataProfileForm
import com.ritense.iko.mvc.model.TraceEvent
import jakarta.validation.Valid
import org.apache.camel.CamelContext
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
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val producerTemplate: ProducerTemplate,
    private val camelContext: CamelContext
) {

    @PostMapping(
        path = ["/aggregated-data-profiles/test"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun test(
        @Valid @ModelAttribute form: AggregatedDataProfileForm,
    ): ModelAndView {
        val tracer = camelContext.camelContextExtension.getContextPlugin(BacklogTracer::class.java)
        requireNotNull(tracer) { "BacklogTracer plugin not found in CamelContext" }
        tracer.clear() // Clean history first

        // Run ADP
        val adp = aggregatedDataProfileRepository.getReferenceById(form.id!!)
        val adpEndpointUri = "direct:aggregated_data_profile_${adp.id}"
        val result = producerTemplate.requestBody(adpEndpointUri, null, String::class.java)

        // Fetch traces
        val traces = tracer.dumpAllTracedMessages()?.map {
            TraceEvent.from(it)
        } ?: emptyList()
        return ModelAndView("$BASE_FRAGMENT_ADG/test").apply {
            addObject("form", form)
            addObject("testResult", result)
            addObject("traces", traces)
        }
    }

}

