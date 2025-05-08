package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.model.AddProfileForm
import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.EditProfileForm
import com.ritense.iko.mvc.model.EditRelationRequest
import com.ritense.iko.mvc.model.MenuItem
import com.ritense.iko.mvc.model.Relation
import com.ritense.iko.mvc.model.Search
import com.ritense.iko.mvc.model.Source
import com.ritense.iko.profile.Profile
import com.ritense.iko.profile.ProfileRepository
import com.ritense.iko.profile.ProfileService
import com.ritense.iko.source.SearchService
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
class InternalMainController(
    private val profileRepository: ProfileRepository,
    private val profileService: ProfileService,
    private val searchService: SearchService,
) {

    val menuItems: List<MenuItem> = listOf(
        MenuItem("Profiles", "/admin/profiles"),
        MenuItem("Searches TODO", "/admin/searches"),
    )

    @GetMapping
    fun home(): ModelAndView {
        val mav = ModelAndView("layout-internal")
        mav.addObject("menuItems", menuItems)
        return mav
    }

    @GetMapping("/profiles")
    fun profileList(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = 10) pageable: Pageable,
        @RequestHeader("Hx-Request") isHxRequest: Boolean = false
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
        @PageableDefault(size = 10) pageable: Pageable
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
        @PageableDefault(size = 10) pageable: Pageable,
        @RequestHeader("Hx-Request") isHxRequest: Boolean = false
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

    @GetMapping("/profiles/edit/{id}")
    fun profileEdit(
        @PathVariable id: UUID,
        @RequestHeader("Hx-Request") isHxRequest: Boolean = false
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
            addObject("relations", relations)
            addObject("menuItems", menuItems)
        }
    }

    @GetMapping("/profiles/create")
    fun profileCreate(): ModelAndView {
        val mav = ModelAndView("fragments/internal/profileAdd")
        return mav
    }

    @PutMapping(path = ["/profiles"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun updateProfile(@ModelAttribute request: EditProfileForm): ModelAndView {
        request.run {
            val profile = profileRepository.getReferenceById(this.id)
            profile.handle(request)
            profileService.reloadRoutes(profile)
            profileRepository.save(profile)
        }
        val mav = ModelAndView("fragments/internal/profileEdit")
        mav.addObject("form", request)
        return mav
    }

    @PostMapping(path = ["/profiles"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun createProfile(@ModelAttribute request: AddProfileForm): ModelAndView {
        val result = request.run {
            val profile = Profile.create(request)
            profileService.reloadRoutes(profile)
            profileRepository.save(profile)
        }
        val mav = ModelAndView("fragments/internal/profileEdit")
        mav.addObject("profile", result)
        return mav
    }

    @GetMapping("/profiles/{id}/relations/create")
    fun relationCreate(@PathVariable id: UUID): ModelAndView {
        val profile = profileRepository.getReferenceById(id)
        val mav = ModelAndView("fragments/internal/relationAdd").apply {
            addObject("profileId", id)
            addObject("sources", profile.relations.map { relation ->
                Source(
                    id = relation.id.toString(),
                    name = relation.id.toString()
                )
            }
            )
            addObject(
                "searches", searchService.getSearches().map {
                    Search(
                        id = it.value,
                        name = it.key,
                    )
                }
            )
        }
        return mav
    }

    @PostMapping("/relations")
    fun createRelation(
        @Valid @ModelAttribute form: AddRelationForm,
        bindingResult: BindingResult,
    ): List<ModelAndView> {
        val profile = profileRepository.getReferenceById(form.profileId)
        val sources = profile.relations.map { relation ->
            Source(
                id = relation.id.toString(),
                name = relation.id.toString()
            )
        }
        val searches = searchService.getSearches().map {
            Search(
                id = it.value,
                name = it.key,
            )
        }

        if (bindingResult.hasErrors()) {
            val fallback = ModelAndView("fragments/internal/relationAdd").apply {
                addObject("profileId", form.profileId)
                addObject("sources", sources)
                addObject("searches", searches)
                addObject("errors", bindingResult)
                addObject("form", form)
            }
            return listOf(fallback)
        }
        val result = form.run {
            profileRepository.getReferenceById(form.profileId).let {
                it.addRelation(form)
                profileService.reloadRoutes(it)
                profileRepository.save(it)
            }
        }
        // Multiple view relationAdd + relations (OOB swap)
        val defaultView = ModelAndView("fragments/internal/relationAdd").apply {
            addObject("profileId", form.profileId)
            addObject("sources", sources)
            addObject("searches", searches)
            addObject("form", form)
        }
        val relations = ModelAndView("fragments/internal/relations").apply {
            addObject("relations", result.relations.map { Relation.from(it) })
        }
        return listOf(
            defaultView,
            relations
        )
    }

    @GetMapping("/profiles/{id}/relations/edit/{relationId}")
    fun relationEdit(
        @PathVariable id: UUID,
        @PathVariable relationId: UUID,
    ): ModelAndView {
        val profile = profileRepository.getReferenceById(id)
        val mav = ModelAndView("fragments/internal/relationEdit").apply {
            addObject("profileId", profile.id)
            addObject("relation", profile.relations.find { it.id == relationId })
        }
        return mav
    }

    @PutMapping("/relations")
    fun updateRelation(@ModelAttribute request: EditRelationRequest): ModelAndView {
        val result = request.run {
            profileRepository.getReferenceById(request.profileId).let {
                it.changeRelation(request)
                profileService.reloadRoutes(it)
                profileRepository.save(it)
            }
        }
        val mav = ModelAndView("fragments/internal/relations").apply {
            addObject(
                "relation",
                result.relations.single { it.id == request.relationId }.let { Relation.from(it) }
            )
        }
        return mav
    }
}