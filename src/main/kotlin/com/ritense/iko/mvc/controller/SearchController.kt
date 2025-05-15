package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.controller.ProfileController.Companion.menuItems
import com.ritense.iko.search.SearchRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView

@Controller
@RequestMapping("/admin")
class SearchController(
    private val searchRepository: SearchRepository
) {

    @GetMapping("/searches")
    fun profileList(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val page = searchRepository.findAll(pageable)
        return if (isHxRequest) {
            ModelAndView("fragments/internal/searchList").apply {
                addObject("searches", page.content)
                addObject("page", page)
                addObject("query", query)
            }
        } else {
            ModelAndView("fragments/internal/searchListPage").apply {
                addObject("searches", page.content)
                addObject("page", page)
                addObject("query", query)
                addObject("menuItems", menuItems)
            }
        }
    }

    @GetMapping("/search-pagination")
    fun pagination(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable
    ): ModelAndView {
        val page = searchRepository.findAll(pageable)
        val list = ModelAndView("fragments/internal/searchPagination").apply {
            addObject("searches", page.content)
            addObject("page", page)
            addObject("query", query)
        }
        return list
    }

    @GetMapping("/search-filter")
    fun searchFilter(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): List<ModelAndView> {
        val page = if (query.isBlank())
            searchRepository.findAll(pageable)
        else
            searchRepository.findByNameContainingIgnoreCase(query.trim(), pageable)

        if (isHxRequest) {
            val searchResults = ModelAndView("fragments/internal/searchFilterResults").apply {
                addObject("searches", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            val pagination = ModelAndView("fragments/internal/searchPagination").apply {
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
                ModelAndView("fragments/internal/searchSearchResultsPage").apply {
                    addObject("searches", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("menuItems", menuItems)
                }
            )
        }
    }

    companion object {
        const val HX_REQUEST_HEADER = "Hx-Request"
        const val PAGE_DEFAULT = 10
    }

}