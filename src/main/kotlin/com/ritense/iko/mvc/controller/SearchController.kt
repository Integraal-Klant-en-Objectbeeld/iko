package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.controller.ProfileController.Companion.menuItems
import com.ritense.iko.mvc.model.EditSearchForm
import com.ritense.iko.mvc.model.Route
import com.ritense.iko.search.SearchRepository
import jakarta.validation.Valid
import org.apache.camel.CamelContext
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
class SearchController(
    private val searchRepository: SearchRepository,
    private val camelContext: CamelContext
) {

    @GetMapping("/searches")
    fun searchesList(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val page = searchRepository.findAll(pageable)
        return if (isHxRequest) {
            ModelAndView("fragments/internal/search/searchList").apply {
                addObject("searches", page.content)
                addObject("page", page)
                addObject("query", query)
            }
        } else {
            ModelAndView("fragments/internal/search/searchListPage").apply {
                addObject("searches", page.content)
                addObject("page", page)
                addObject("query", query)
                addObject("menuItems", menuItems)
            }
        }
    }

    @GetMapping("/searches/pagination")
    fun searchesPagination(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable
    ): ModelAndView {
        val page = searchRepository.findAll(pageable)
        val list = ModelAndView("fragments/internal/search/searchPagination").apply {
            addObject("searches", page.content)
            addObject("page", page)
            addObject("query", query)
        }
        return list
    }

    @GetMapping("/searches/filter")
    fun searchesFilter(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): List<ModelAndView> {
        val page = if (query.isBlank())
            searchRepository.findAll(pageable)
        else
            searchRepository.findByNameContainingIgnoreCase(query.trim(), pageable)

        if (isHxRequest) {
            val searchResults = ModelAndView("fragments/internal/search/searchFilterResults").apply {
                addObject("searches", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            val pagination = ModelAndView("fragments/internal/search/searchPagination").apply {
                addObject("searches", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            return listOf(
                searchResults,
                pagination
            )
        } else {
            return listOf(
                ModelAndView("fragments/internal/search/searchSearchResultsPage").apply {
                    addObject("searches", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("menuItems", menuItems)
                }
            )
        }
    }

    @GetMapping("/searches/edit/{id}")
    fun searchEdit(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val search = searchRepository.getReferenceById(id)
        val form = EditSearchForm.from(search)
        val viewName = if (isHxRequest) {
            "fragments/internal/search/searchEdit"
        } else {
            "fragments/internal/search/searchEditPage"
        }
        return ModelAndView(viewName).apply {
            addObject("form", form)
            addObject("routes", routes())
            addObject("menuItems", menuItems)
        }
    }

    @PutMapping(path = ["/searches"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun updateSearch(
        @Valid @ModelAttribute form: EditSearchForm,
        bindingResult: BindingResult
    ): ModelAndView {
        val search = searchRepository.getReferenceById(form.id)
        val modelAndView = ModelAndView("fragments/internal/search/searchEdit").apply {
            addObject("errors", bindingResult)
            addObject("form", form)
            addObject("routes", routes())
        }
        if (bindingResult.hasErrors()) {
            return modelAndView
        }
        val result = form.run {
            search.handle(form)
            searchRepository.save(search)
        }
        return modelAndView
    }

    // TODO this should go? because the list of searches is "direct":URI -> these get configured at startup.
    // Thoughts: The table searches is a manual list that CAN connect to beans present in the camelcontext
    // now this only all the beans on startup being added.
    // Having a table in between makes it a effort to OPEN a search for real use. Then the beans could already be known.
    // So routes() could just return all the beans annotated with @Search to make a list.
    private fun routes() = camelContext.routes
        .filter { it.routeId.startsWith("direct:") && it.routeId != "direct:direct:" }
        .map {
            Route(
                id = it.id.toString(),
                name = it.routeId,
            )
        }

    companion object {
        const val HX_REQUEST_HEADER = "Hx-Request"
        const val PAGE_DEFAULT = 10
    }

}