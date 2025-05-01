package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.model.CreateProfileRequest
import com.ritense.iko.mvc.model.CreateRelationRequest
import com.ritense.iko.mvc.model.EditRelationRequest
import com.ritense.iko.mvc.model.MenuItem
import com.ritense.iko.mvc.model.ModifyProfileRequest
import com.ritense.iko.profile.Profile
import com.ritense.iko.profile.ProfileRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
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
    private val profileRepository: ProfileRepository
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
        @PathVariable id: String,
        @RequestHeader("Hx-Request") isHxRequest: Boolean = false
    ): ModelAndView {
        val profile = profileRepository.getReferenceById(UUID.fromString(id))
        return if (isHxRequest) {
            ModelAndView("fragments/internal/profileEdit").apply {
                addObject("profile", profile)
                addObject("menuItems", menuItems)
            }
        } else {
            ModelAndView("fragments/internal/profileEditPage").apply {
                addObject("profile", profile)
                addObject("menuItems", menuItems)
            }
        }
    }

    @GetMapping("/profiles/create")
    fun profileCreate(): ModelAndView {
        val mav = ModelAndView("fragments/internal/profileAdd")
        return mav
    }

    @PutMapping(path = ["/profiles"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun updateProfile(@ModelAttribute request: ModifyProfileRequest): ModelAndView {
        val result = request.run {
            val profile = profileRepository.getReferenceById(this.id)
            profile.handle(request)
            profileRepository.save(profile)
        }
        val mav = ModelAndView("fragments/internal/profileEdit")
        mav.addObject("profile", result)
        return mav
    }

    @PostMapping(path = ["/profiles"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun createProfile(@ModelAttribute request: CreateProfileRequest): ModelAndView {
        val result = request.run {
            profileRepository.save(Profile.create(request))
        }
        val mav = ModelAndView("fragments/internal/profileEdit")
        mav.addObject("profile", result)
        return mav
    }

    @GetMapping("/profiles/{id}/relations/create")
    fun relationCreate(@PathVariable id: UUID): ModelAndView {
        val mav = ModelAndView("fragments/internal/relationAdd").apply {
            addObject("profileId", id)
        }
        return mav
    }

    @PostMapping("/relations")
    fun createRelation(@ModelAttribute request: CreateRelationRequest): ModelAndView {
        val result = request.run {
            profileRepository.getReferenceById(request.profileId).let {
                it.addRelation(request)
                profileRepository.save(it)
            }
        }
        val mav = ModelAndView("fragments/internal/relations").apply {
            addObject("profile", result)
        }
        return mav
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
                profileRepository.save(it)
            }
        }
        val mav = ModelAndView("fragments/internal/relations").apply {
            addObject("profile", result)
        }
        return mav
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
                profileRepository.save(it)
            }
        }
        val mav = ModelAndView("fragments/internal/relations").apply {
            addObject("profile", result)
            addObject("relation", result.relations.find { it.id == request.relationId })
        }
        return mav
    }
}