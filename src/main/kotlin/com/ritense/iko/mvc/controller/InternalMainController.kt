package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.model.MenuItem
import com.ritense.iko.mvc.service.PersonClientService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView

@Controller
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
        model: Model
    ): ModelAndView {
        val mav = ModelAndView("fragments/internal/profileList")
        mav.addObject("profiles", profiles)
        return mav
    }

    // this is FUBAR renaming this to /profiles/search
    // WONT WORK response is 200 but no HTML is rendered.
    // Clash with apache camel most likely
    @GetMapping("/profile-search")
    fun searchResults(
        @RequestParam query: String,
        model: Model
    ): ModelAndView {
        val filteredProfiles = if (query.isBlank()) {
            profiles
        } else {
            profiles.filter { it.name.contains(query, ignoreCase = true) }
        }
        val mav = ModelAndView("fragments/internal/profileSearchResults")
        mav.addObject("profiles", filteredProfiles)
        return mav
    }

    @GetMapping("/profiles/view/{id}")
    fun profileDetail(@PathVariable id: String, model: Model): ModelAndView {
        val mav = ModelAndView("fragments/internal/profileDetail")
        mav.addObject("profile", profiles.first { it.id == id })
        return mav
    }
}

data class Profile(
    val id: String,
    val name: String
)