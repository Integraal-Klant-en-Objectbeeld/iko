package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.model.MenuItem
import com.ritense.iko.mvc.service.PersonClientService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView

@Controller()
class InternalMainController(
    private val personClientService: PersonClientService,
) {

    var profiles: List<Profile> = (1..500).map { i ->
        Profile(id = i.toString(), name = "Profile $i")
    }

    @GetMapping("/")
    fun home(): ModelAndView {
        val mav = ModelAndView("layout-internal")
        mav.addObject(
            "menuItems",
            listOf(
                MenuItem("Profiles", "/profiles"),
                MenuItem("Searches", "/searches"),
            )
        )
        return mav
    }

    @GetMapping("/profiles")
    fun profileList(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        model: Model
    ): ModelAndView {
        val mav = ModelAndView("fragments/internal/profileList")

        // Calculate pagination values
        val totalElements = profiles.size
        val totalPages = (totalElements + size - 1) / size
        val currentPage = page.coerceIn(0, maxOf(0, totalPages - 1))
        val startIndex = currentPage * size
        val endIndex = minOf(startIndex + size, totalElements)

        // Get paginated profiles
        val paginatedProfiles = profiles.subList(startIndex, endIndex)

        // Add pagination data to the model
        mav.addObject("profiles", paginatedProfiles)
        mav.addObject("currentPage", currentPage)
        mav.addObject("totalPages", totalPages)
        mav.addObject("totalElements", totalElements)
        mav.addObject("pageSize", size)
        return mav
    }

    @GetMapping("/profiles/search")
    fun searchResults(@RequestParam search: String, model: Model): ModelAndView {
        val mav = ModelAndView("fragments/internal/profileList")
        mav.addObject("profiles", profiles.find { it.name.contains(search) })
        return mav
    }

    @GetMapping("/profiles/view/{id}")
    fun profileDetail(@PathVariable id: String, model: Model): ModelAndView {
        val mav = ModelAndView("fragments/internal/profileDetail")
        mav.addObject("profile", profiles.first { it.id == id })
        return mav
    }

    @GetMapping("/htmx/pagination")
    fun getPagination(
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "5") size: Int,
        model: Model
    ): String {
        // Calculate pagination values
        val totalElements = profiles.size
        val totalPages = (totalElements + size - 1) / size
        val currentPage = page.coerceIn(0, maxOf(0, totalPages - 1))
        val startIndex = currentPage * size
        val endIndex = minOf(startIndex + size, totalElements)

        // Get paginated profiles
        val paginatedProfiles = profiles.subList(startIndex, endIndex)
        model.addAttribute("profiles", paginatedProfiles)
        model.addAttribute("currentPage", currentPage)
        model.addAttribute("totalPages", totalPages)
        model.addAttribute("totalElements", totalElements)
        model.addAttribute("pageSize", size)

        return "person-crud-htmx :: pagination"
    }
}

data class Profile(
    val id: String,
    val name: String
)