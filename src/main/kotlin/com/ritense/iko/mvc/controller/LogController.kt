/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
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

import com.ritense.iko.logging.service.LoggingEventService
import com.ritense.iko.mvc.controller.ConnectorController.Companion.hxRequest
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_LOGGING
import com.ritense.iko.mvc.controller.HomeController.Companion.HX_REQUEST_HEADER
import com.ritense.iko.mvc.model.LoggingFilter
import com.ritense.iko.security.SecurityContextHelper
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

@Controller
@RequestMapping("/admin/logs")
internal class LogController(
    private val loggingEventService: LoggingEventService,
) {
    @GetMapping
    fun list(
        filter: LoggingFilter,
        @PageableDefault(size = LOG_PAGE_DEFAULT, sort = ["timestamp"], direction = DESC) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView = if (isHxRequest) {
        val page = loggingEventService.search(filter, pageable)
        ModelAndView("$BASE_FRAGMENT_LOGGING/list").apply {
            addObject("events", page.content)
            addObject("page", page)
            addObject("filter", filter)
        }
    } else {
        val page = loggingEventService.search(filter, pageable)
        ModelAndView("$BASE_FRAGMENT_LOGGING/list-page-logging").apply {
            addObject("events", page.content)
            addObject("page", page)
            addObject("filter", filter)
            addObject("username", SecurityContextHelper.getUserPropertyByKey("name"))
            addObject("email", SecurityContextHelper.getUserPropertyByKey("email"))
        }
    }

    @GetMapping("/filter")
    fun filter(
        filter: LoggingFilter,
        @PageableDefault(size = LOG_PAGE_DEFAULT, sort = ["timestamp"], direction = DESC) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): List<ModelAndView> {
        val page = loggingEventService.search(filter, pageable)
        return if (isHxRequest) {
            val searchResults =
                ModelAndView("$BASE_FRAGMENT_LOGGING/filter-results").apply {
                    addObject("events", page.content)
                    addObject("page", page)
                    addObject("filter", filter)
                }
            val pagination =
                ModelAndView("$BASE_FRAGMENT_LOGGING/pagination").apply {
                    addObject("events", page.content)
                    addObject("page", page)
                    addObject("filter", filter)
                }
            listOf(searchResults, pagination)
        } else {
            listOf(
                ModelAndView("$BASE_FRAGMENT_LOGGING/list-page-logging").apply {
                    addObject("events", page.content)
                    addObject("page", page)
                    addObject("filter", filter)
                    addObject("username", SecurityContextHelper.getUserPropertyByKey("name"))
                    addObject("email", SecurityContextHelper.getUserPropertyByKey("email"))
                },
            )
        }
    }

    @GetMapping("/{eventId}")
    fun detail(
        @PathVariable eventId: Long,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        val event = loggingEventService.findById(eventId)
        return hxRequest(
            isHxRequest,
            "$BASE_FRAGMENT_LOGGING/detail-modal",
            "logging-detail-modal",
            mapOf("event" to event),
        )
    }

    companion object {
        const val LOG_PAGE_DEFAULT = 50
    }
}