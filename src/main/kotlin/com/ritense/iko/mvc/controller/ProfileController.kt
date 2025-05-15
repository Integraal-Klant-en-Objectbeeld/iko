package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.model.AddProfileForm
import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.EditProfileForm
import com.ritense.iko.mvc.model.EditRelationForm
import com.ritense.iko.mvc.model.MenuItem
import com.ritense.iko.mvc.model.Relation
import com.ritense.iko.mvc.model.Search
import com.ritense.iko.mvc.model.Source
import com.ritense.iko.profile.Profile
import com.ritense.iko.profile.ProfileRepository
import com.ritense.iko.profile.ProfileService
import com.ritense.iko.search.SearchService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import java.util.UUID

@Controller
@RequestMapping("/admin")
class ProfileController(
    private val profileRepository: ProfileRepository,
    private val profileService: ProfileService,
    private val searchService: SearchService,
) {

    @GetMapping
    fun home(): ModelAndView {
        val mav = ModelAndView("layout-internal")
        mav.addObject("menuItems", menuItems)
        return mav
    }

    @GetMapping("/profiles")
    fun profileList(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val page = profileRepository.findAll(pageable)
        return if (isHxRequest) {
            ModelAndView("fragments/internal/profileList").apply {
                addObject("profiles", page.content)
                addObject("page", page)
                addObject("query", query)
            }
        } else {
            ModelAndView("fragments/internal/profileListPage").apply {
                addObject("profiles", page.content)
                addObject("page", page)
                addObject("query", query)
                addObject("menuItems", menuItems)
            }
        }
    }

    @GetMapping("/pagination")
    fun pagination(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable
    ): ModelAndView {
        val page = profileRepository.findAll(pageable)
        val list = ModelAndView("fragments/internal/pagination").apply {
            addObject("profiles", page.content)
            addObject("page", page)
            addObject("query", query)
        }
        return list
    }

    @GetMapping("/profile-search")
    fun searchResults(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): List<ModelAndView> {
        val page = if (query.isBlank())
            profileRepository.findAll(pageable)
        else
            profileRepository.findByNameContainingIgnoreCase(query.trim(), pageable)

        if (isHxRequest) {
            val searchResults = ModelAndView("fragments/internal/profileSearchResults").apply {
                addObject("profiles", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            val pagination = ModelAndView("fragments/internal/pagination").apply {
                addObject("profiles", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            return listOf(
                searchResults,
                pagination
            )
        } else {
            return listOf(
                ModelAndView("fragments/internal/profileSearchResultsPage").apply {
                    addObject("profiles", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("menuItems", menuItems)
                }
            )
        }
    }

    @GetMapping("/profiles/create")
    fun profileCreate(): ModelAndView {
        val searches = primarySearches()
        val modelAndView = ModelAndView("fragments/internal/profileAdd").apply {
            addObject("searches", searches)
        }
        return modelAndView
    }

    @PostMapping(path = ["/profiles"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun createProfile(
        @Valid @ModelAttribute form: AddProfileForm,
        bindingResult: BindingResult
    ): ModelAndView {
        val searches = primarySearches()
        val modelAndView = ModelAndView("fragments/internal/profileAdd").apply {
            addObject("form", form)
            addObject("searches", searches)
            addObject("errors", bindingResult)
        }
        if (bindingResult.hasErrors()) {
            return modelAndView
        }
        val profile = form.run {
            val profile = Profile.create(form)
            profileService.reloadRoutes(profile)
            profileRepository.save(profile)
        }
        val redirectModelAndView = ModelAndView("fragments/internal/profileEdit").apply {
            addObject("form", EditProfileForm.from(profile))
            addObject("searches", searches)
            addObject("relations", profile.relations.map { Relation.from(it) })
        }
        return redirectModelAndView
    }

    @GetMapping("/profiles/edit/{id}")
    fun profileEdit(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val profile = profileRepository.getReferenceById(id)
        val form = EditProfileForm.from(profile)
        val relations = profile.relations.map { Relation.from(it) }
        val viewName = if (isHxRequest) {
            "fragments/internal/profileEdit"
        } else {
            "fragments/internal/profileEditPage"
        }
        return ModelAndView(viewName).apply {
            addObject("form", form)
            addObject("searches", primarySearches())
            addObject("relations", relations)
            addObject("menuItems", menuItems)
        }
    }

    @PutMapping(path = ["/profiles"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun updateProfile(
        @Valid @ModelAttribute form: EditProfileForm,
        bindingResult: BindingResult
    ): ModelAndView {
        val profile = profileRepository.getReferenceById(form.id)
        val modelAndView = ModelAndView("fragments/internal/profileEdit").apply {
            addObject("errors", bindingResult)
            addObject("form", form)
            addObject("relations", profile.relations.map { Relation.from(it) })
            addObject("searches", primarySearches())
        }
        if (bindingResult.hasErrors()) {
            return modelAndView
        }
        val profileUpdated = form.run {
            profile.handle(form)
            profileService.reloadRoutes(profile)
            profileRepository.save(profile)
        }
        return modelAndView
    }

    @GetMapping("/profiles/{id}/relations/create")
    fun relationCreate(@PathVariable id: UUID): ModelAndView {
        val profile = profileRepository.getReferenceById(id)
        val sources = sources(profile)
        val searches = searches()
        val modelAndView = ModelAndView("fragments/internal/relationAdd").apply {
            addObject("profileId", id)
            addObject("sources", sources)
            addObject("searches", searches)
        }
        return modelAndView
    }

    @PostMapping("/relations")
    fun createRelation(
        @Valid @ModelAttribute form: AddRelationForm,
        bindingResult: BindingResult,
    ): List<ModelAndView> {
        val profile = profileRepository.getReferenceById(form.profileId)
        val sources = sources(profile)
        val searches = searches()
        val modelAndView = ModelAndView("fragments/internal/relationAdd").apply {
            addObject("profileId", form.profileId)
            addObject("sources", sources)
            addObject("searches", searches)
            addObject("errors", bindingResult)
            addObject("form", form)
        }
        if (bindingResult.hasErrors()) {
            return listOf(modelAndView)
        }
        val result = form.run {
            profile.let {
                it.addRelation(form)
                profileService.reloadRoutes(it)
                profileRepository.save(it)
            }
        }
        val relationsModelAndView = ModelAndView("fragments/internal/relations").apply {
            addObject("relations", result.relations.map { Relation.from(it) })
        }
        return listOf(
            modelAndView,
            relationsModelAndView // OOB Swap
        )
    }

    @GetMapping("/profiles/{id}/relations/edit/{relationId}")
    fun relationEdit(
        @PathVariable id: UUID,
        @PathVariable relationId: UUID,
    ): ModelAndView {
        val profile = profileRepository.getReferenceById(id)
        val sources = sources(profile).apply { this.removeIf { it.id == relationId.toString() } }
        val searches = searches()
        val modelAndView = ModelAndView("fragments/internal/relationEdit").apply {
            addObject("sources", sources)
            addObject("searches", searches)
            addObject("form", profile.relations.find { it.id == relationId }?.let { EditRelationForm.from(it) })
        }
        return modelAndView
    }

    @PutMapping("/relations")
    fun updateRelation(
        @Valid @ModelAttribute form: EditRelationForm,
        bindingResult: BindingResult,
    ): List<ModelAndView> {
        val profile = profileRepository.getReferenceById(form.profileId)
        val sources = sources(profile).apply { this.removeIf { it.id == form.id.toString() } }
        val searches = searches()
        val modelAndView = ModelAndView("fragments/internal/relationEdit").apply {
            addObject("profileId", form.profileId)
            addObject("sources", sources)
            addObject("searches", searches)
            addObject("errors", bindingResult)
            addObject("form", form)
        }
        if (bindingResult.hasErrors()) {
            return listOf(modelAndView)
        }
        val updatedProfile = form.run {
            profile.let {
                it.changeRelation(form)
                profileService.reloadRoutes(it)
                profileRepository.save(it)
            }
        }
        val relationsModelAndView = ModelAndView("fragments/internal/relations").apply {
            addObject("relations", updatedProfile.relations.map { Relation.from(it) })
        }
        return listOf(
            modelAndView,
            relationsModelAndView // OOB Swap
        )
    }

    private fun primarySearches() = searchService.getPrimarySearches().map {
        Search(
            id = it.id.toString(),
            name = it.name,
        )
    }

    private fun searches() = searchService.getSearches().map {
        Search(
            id = it.id.toString(),
            name = it.name,
        )
    }

    private fun sources(profile: Profile) = profile.relations.map { relation ->
        Source(
            id = relation.id.toString(),
            name = relation.id.toString()
        )
    }.toMutableList()

    companion object {
        const val HX_REQUEST_HEADER = "Hx-Request"
        const val PAGE_DEFAULT = 10
        val menuItems: List<MenuItem> = listOf(
            MenuItem("Profiles", "/admin/profiles"),
            MenuItem("Searches", "/admin/searches"),
        )
    }

}