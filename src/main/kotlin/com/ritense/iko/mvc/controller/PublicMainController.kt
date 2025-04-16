package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.model.Search
import com.ritense.iko.mvc.service.PersonClientService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView

@Controller
class PublicMainController(
    private val personClientService: PersonClientService,
) {

    @GetMapping("/public")
    fun home(): ModelAndView {
        val mav = ModelAndView("layout-public")
        mav.addObject("fragment", "fragments/public/search")
        mav.addObject("view", "public/search")
        mav.addObject(
            "searches",
            listOf(
                Search(1, "Personen BSN"),
                Search(2, "Zaken")
            )
        )
        return mav
    }

    @GetMapping("/public/search/{name}")
    fun searchPage(@PathVariable name: String, model: Model): ModelAndView {
        val mav = ModelAndView("fragments/public/search")
        mav.addObject("name", name)
        return mav
    }

    @GetMapping("/public/search")
    fun searchResults(@RequestParam search: String, model: Model): String {
        val personen = personClientService.searchPersonByBSN(search)
        model.addAttribute("results", personen)
        return "fragments/public/searchResults :: search-results"
    }

    @GetMapping("/public/contacts/{id}")
    fun contactDetails(@PathVariable id: Long, model: Model): String {
        model.addAttribute("contact", "contact")
        return "fragments/public/contactDetails :: contact"
    }

}