package com.ritense.iko.mvc.controller

import com.ritense.iko.endpoints.EndpointRepository
import com.ritense.iko.mvc.controller.ProfileController.Companion.menuItems
import com.ritense.iko.mvc.model.EditEndpointForm
import com.ritense.iko.mvc.model.Route
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import java.util.UUID

@Controller
@RequestMapping("/admin")
class ApiEndpointController(
    private val endpointRepository: EndpointRepository
) {

    @GetMapping("/endpoints")
    fun list(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val page = endpointRepository.findAll(pageable)
        return if (isHxRequest) {
            ModelAndView("fragments/internal/endpoint/list").apply {
                addObject("endpoints", page.content)
                addObject("page", page)
                addObject("query", query)
            }
        } else {
            ModelAndView("fragments/internal/endpoint/listPage").apply {
                addObject("endpoints", page.content)
                addObject("page", page)
                addObject("query", query)
                addObject("menuItems", menuItems)
            }
        }
    }

    @GetMapping("/endpoints/pagination")
    fun pagination(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable
    ): ModelAndView {
        val page = endpointRepository.findAll(pageable)
        val list = ModelAndView("fragments/internal/endpoint/pagination").apply {
            addObject("endpoints", page.content)
            addObject("page", page)
            addObject("query", query)
        }
        return list
    }

    @GetMapping("/endpoints/filter")
    fun filter(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): List<ModelAndView> {
        val page = if (query.isBlank())
            endpointRepository.findAll(pageable)
        else
            endpointRepository.findByNameContainingIgnoreCase(query.trim(), pageable)

        if (isHxRequest) {
            val searchResults = ModelAndView("fragments/internal/endpoint/filterResults").apply {
                addObject("endpoints", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            val pagination = ModelAndView("fragments/internal/endpoint/pagination").apply {
                addObject("endpoints", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            return listOf(
                searchResults,
                pagination
            )
        } else {
            return listOf(
                ModelAndView("fragments/internal/endpoint/filterResultsPage").apply {
                    addObject("endpoints", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("menuItems", menuItems)
                }
            )
        }
    }

    @GetMapping("/endpoints/edit/{id}")
    fun edit(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val endpoint = endpointRepository.getReferenceById(id)
        val form = EditEndpointForm.from(endpoint)
        val viewName = if (isHxRequest) {
            "fragments/internal/endpoint/edit"
        } else {
            "fragments/internal/endpoint/editPage"
        }
        return ModelAndView(viewName).apply {
            addObject("form", form)
            addObject("routes", routes())
            addObject("menuItems", menuItems)
        }
    }

    @PutMapping(path = ["/endpoints"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun edit(
        @Valid @ModelAttribute form: EditEndpointForm,
        bindingResult: BindingResult
    ): ModelAndView {
        val endpoint = endpointRepository.getReferenceById(form.id)
        val modelAndView = ModelAndView("fragments/internal/endpoint/edit").apply {
            addObject("errors", bindingResult)
            addObject("form", form)
            addObject("routes", routes())
        }
        if (bindingResult.hasErrors()) {
            return modelAndView
        }
        endpoint.handle(form)
        endpointRepository.save(endpoint)
        return modelAndView
    }

    private fun routes() = endpointRepository.findAll()
        .map {
            Route(
                id = it.routeId,
                name = it.name,
            )
        }

    companion object {
        const val HX_REQUEST_HEADER = "Hx-Request"
        const val PAGE_DEFAULT = 10
    }

}