package com.ritense.iko.mvc.controller

import com.ritense.iko.endpoints.EndpointService
import com.ritense.iko.mvc.model.AddProfileForm
import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.EditProfileForm
import com.ritense.iko.mvc.model.EditRelationForm
import com.ritense.iko.mvc.model.Endpoint
import com.ritense.iko.mvc.model.MenuItem
import com.ritense.iko.mvc.model.Relation
import com.ritense.iko.mvc.model.Source
import com.ritense.iko.profile.Profile
import com.ritense.iko.profile.ProfileRepository
import com.ritense.iko.profile.ProfileService
import jakarta.validation.Valid
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BindingResult
import org.springframework.validation.ObjectError
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
    private val endpointService: EndpointService,
) {

    @GetMapping
    fun home(): ModelAndView {
        val mav = ModelAndView("layout-internal")
        mav.addObject("menuItems", menuItems)
        return mav
    }

    @GetMapping("/profiles")
    fun list(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val page = profileRepository.findAllBy(pageable)
        return if (isHxRequest) {
            ModelAndView("fragments/internal/profile/list").apply {
                addObject("profiles", page.content)
                addObject("page", page)
                addObject("query", query)
            }
        } else {
            ModelAndView("fragments/internal/profile/listPage").apply {
                addObject("profiles", page.content)
                addObject("page", page)
                addObject("query", query)
                addObject("menuItems", menuItems)
            }
        }
    }

    @GetMapping("/profiles/pagination")
    fun pagination(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable
    ): ModelAndView {
        val page = profileRepository.findAll(pageable)
        val list = ModelAndView("fragments/internal/profile/pagination").apply {
            addObject("profiles", page.content)
            addObject("page", page)
            addObject("query", query)
        }
        return list
    }

    @GetMapping("/profiles/filter")
    fun filter(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): List<ModelAndView> {
        val page = if (query.isBlank())
            profileRepository.findAllBy(pageable)
        else
            profileRepository.findAllByName(query.trim(), pageable)

        if (isHxRequest) {
            val searchResults = ModelAndView("fragments/internal/profile/filterResults").apply {
                addObject("profiles", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            val pagination = ModelAndView("fragments/internal/profile/pagination").apply {
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
                ModelAndView("fragments/internal/profile/filterResultsPage").apply {
                    addObject("profiles", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("menuItems", menuItems)
                }
            )
        }
    }

    @GetMapping("/profiles/create")
    fun create(): ModelAndView {
        val endpoints = primaryEndpoints()
        val modelAndView = ModelAndView("fragments/internal/profile/add").apply {
            addObject("endpoints", endpoints)
        }
        return modelAndView
    }

    @PostMapping(path = ["/profiles"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @Transactional
    fun create(
        @Valid @ModelAttribute form: AddProfileForm,
        bindingResult: BindingResult
    ): ModelAndView {
        val endpoints = primaryEndpoints()
        val modelAndView = ModelAndView("fragments/internal/profile/add").apply {
            addObject("form", form)
            addObject("endpoints", endpoints)
            addObject("errors", bindingResult)
        }
        if (bindingResult.hasErrors()) {
            return modelAndView
        }
        val profile = Profile.create(form)
        profileRepository.saveAndFlush(profile)
        profileService.reloadRoutes(profile)
        val redirectModelAndView = ModelAndView("fragments/internal/profile/edit").apply {
            addObject("form", EditProfileForm.from(profile))
            addObject("endpoints", endpoints)
            addObject("relations", profile.relations.map { Relation.from(it) })
        }
        return redirectModelAndView
    }

    @GetMapping("/profiles/edit/{id}")
    fun edit(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val profile = profileRepository.getReferenceById(id)
        val form = EditProfileForm.from(profile)
        val relations = profile.relations.map { Relation.from(it) }
        val viewName = if (isHxRequest) {
            "fragments/internal/profile/edit"
        } else {
            "fragments/internal/profile/editPage"
        }
        return ModelAndView(viewName).apply {
            addObject("form", form)
            addObject("endpoints", primaryEndpoints())
            addObject("relations", relations)
            addObject("menuItems", menuItems)
        }
    }

    @PutMapping(path = ["/profiles"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun edit(
        @Valid @ModelAttribute form: EditProfileForm,
        bindingResult: BindingResult
    ): ModelAndView {
        val profile = profileRepository.getReferenceById(form.id)
        val modelAndView = ModelAndView("fragments/internal/profile/edit").apply {
            addObject("errors", bindingResult)
            addObject("form", form)
            addObject("relations", profile.relations.map { Relation.from(it) })
            addObject("endpoints", primaryEndpoints())
        }
        if (bindingResult.hasErrors()) {
            return modelAndView
        }
        profile.handle(form)
        profileService.reloadRoutes(profile)
        profileRepository.save(profile)
        return modelAndView
    }

    @GetMapping("/profiles/{id}/relations/create")
    fun relationCreate(@PathVariable id: UUID): ModelAndView {
        val profile = profileRepository.getReferenceById(id)
        val sources = sources(profile)
        val endpoints = endpoints()
        val modelAndView = ModelAndView("fragments/internal/relation/add").apply {
            addObject("profileId", id)
            addObject("sources", sources)
            addObject("endpoints", endpoints)
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
        val endpoints = endpoints()
        val modelAndView = ModelAndView("fragments/internal/relation/add").apply {
            addObject("profileId", form.profileId)
            addObject("sources", sources)
            addObject("endpoints", endpoints)
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
        val relationsModelAndView = ModelAndView("fragments/internal/relation/list").apply {
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
        val endpoints = endpoints()
        val modelAndView = ModelAndView("fragments/internal/relation/edit").apply {
            addObject("sources", sources)
            addObject("endpoints", endpoints)
            addObject("form", profile.relations.find { it.id == relationId }?.let { EditRelationForm.from(it) })
        }
        return modelAndView
    }

    @PutMapping("/relations")
    fun editRelation(
        @Valid @ModelAttribute form: EditRelationForm,
        bindingResult: BindingResult,
    ): List<ModelAndView> {
        val profile = profileRepository.getReferenceById(form.profileId)
        val sources = sources(profile).apply { this.removeIf { it.id == form.id.toString() } }
        val endpoints = endpoints()
        val modelAndView = ModelAndView("fragments/internal/relation/edit").apply {
            addObject("profileId", form.profileId)
            addObject("sources", sources)
            addObject("endpoints", endpoints)
            addObject("errors", bindingResult)
            addObject("form", form)
        }
        if (bindingResult.hasErrors()) {
            return listOf(modelAndView)
        }
        var updatedProfile: Profile? = null
        try {
            updatedProfile = form.run {
                profile.let {
                    it.changeRelation(form)
                    profileService.reloadRoutes(it)
                    profileRepository.save(it)
                }
            }
        } catch (ex: DataIntegrityViolationException) {
            bindingResult.addError(ObjectError("name", "A profile with this name already exists."))
            return listOf(modelAndView)
        }
        val relationsModelAndView = ModelAndView("fragments/internal/relation/list").apply {
            addObject("relations", updatedProfile.relations.map { Relation.from(it) })
        }
        return listOf(
            modelAndView,
            relationsModelAndView // OOB Swap
        )
    }

    private fun primaryEndpoints() = endpointService.getPrimaryEndpoints().map {
        Endpoint(
            id = it.id.toString(),
            name = it.name,
        )
    }

    private fun endpoints() = endpointService.getEndpoints().map {
        Endpoint(
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
            MenuItem("Aggregated Data Profiles", "/admin/profiles"),
            MenuItem("API Endpoints", "/admin/endpoints"),
        )
    }

}