package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.model.MenuItem
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

@Controller
@RequestMapping("/admin")
class HomeController {

    @GetMapping
    fun home(): ModelAndView {
        val home = ModelAndView("layout-internal").apply {
            addObject("menuItems", menuItems)
        }
        return home
    }

    companion object {
        const val HX_REQUEST_HEADER = "Hx-Request"
        const val PAGE_DEFAULT = 10
        const val BASE_FRAGMENT_ADG = "fragments/internal/aggregated-data-profile"
        const val BASE_FRAGMENT_RELATION = "fragments/internal/relation"

        val menuItems: List<MenuItem> = listOf(
            MenuItem("Aggregated Data Profiles", "/admin/aggregated-data-profiles"),
            MenuItem("API Endpoints", "/admin/endpoints"),
        )
    }

}