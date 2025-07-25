package com.ritense.iko.mvc.controller

import com.ritense.iko.aggregateddataprofile.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.AggregatedDataProfileRepository
import com.ritense.iko.aggregateddataprofile.AggregatedDataProfileService
import com.ritense.iko.endpoints.EndpointService
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_ADG
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_RELATION
import com.ritense.iko.mvc.controller.HomeController.Companion.HX_REQUEST_HEADER
import com.ritense.iko.mvc.controller.HomeController.Companion.PAGE_DEFAULT
import com.ritense.iko.mvc.controller.HomeController.Companion.menuItems
import com.ritense.iko.mvc.model.AddAggregatedDataProfileForm
import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.EditAggregatedDataProfileForm
import com.ritense.iko.mvc.model.EditRelationForm
import com.ritense.iko.mvc.model.Endpoint
import com.ritense.iko.mvc.model.Relation
import com.ritense.iko.mvc.model.Source
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
class AggregatedDataProfileController(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val aggregatedDataProfileService: AggregatedDataProfileService,
    private val endpointService: EndpointService,
) {

    @GetMapping("/aggregated-data-profiles")
    fun list(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val page = aggregatedDataProfileRepository.findAllBy(pageable)
        return if (isHxRequest) {
            ModelAndView("$BASE_FRAGMENT_ADG/list").apply {
                addObject("aggregatedDataProfiles", page.content)
                addObject("page", page)
                addObject("query", query)
            }
        } else {
            ModelAndView("$BASE_FRAGMENT_ADG/listPage").apply {
                addObject("aggregatedDataProfiles", page.content)
                addObject("page", page)
                addObject("query", query)
                addObject("menuItems", menuItems)
            }
        }
    }

    @GetMapping("/aggregated-data-profiles/pagination")
    fun pagination(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable
    ): ModelAndView {
        val page = aggregatedDataProfileRepository.findAll(pageable)
        val list = ModelAndView("$BASE_FRAGMENT_ADG/pagination").apply {
            addObject("aggregatedDataProfiles", page.content)
            addObject("page", page)
            addObject("query", query)
        }
        return list
    }

    @GetMapping("/aggregated-data-profiles/filter")
    fun filter(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): List<ModelAndView> {
        val page = if (query.isBlank())
            aggregatedDataProfileRepository.findAllBy(pageable)
        else
            aggregatedDataProfileRepository.findAllByName(query.trim(), pageable)

        if (isHxRequest) {
            val searchResults = ModelAndView("$BASE_FRAGMENT_ADG/filterResults").apply {
                addObject("aggregatedDataProfiles", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            val pagination = ModelAndView("$BASE_FRAGMENT_ADG/pagination").apply {
                addObject("aggregatedDataProfiles", page.content)
                addObject("page", page)
                addObject("query", query)
            }
            return listOf(
                searchResults,
                pagination
            )
        } else {
            return listOf(
                ModelAndView("$BASE_FRAGMENT_ADG/filterResultsPage").apply {
                    addObject("aggregatedDataProfiles", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("menuItems", menuItems)
                }
            )
        }
    }

    @GetMapping("/aggregated-data-profiles/create")
    fun create(): ModelAndView {
        val endpoints = primaryEndpoints()
        val modelAndView = ModelAndView("$BASE_FRAGMENT_ADG/add").apply {
            addObject("endpoints", endpoints)
        }
        return modelAndView
    }

    @PostMapping(
        path = ["/aggregated-data-profiles"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    @Transactional
    fun create(
        @Valid @ModelAttribute form: AddAggregatedDataProfileForm,
        bindingResult: BindingResult
    ): ModelAndView {
        val endpoints = primaryEndpoints()
        val modelAndView = ModelAndView("$BASE_FRAGMENT_ADG/add").apply {
            addObject("form", form)
            addObject("endpoints", endpoints)
            addObject("errors", bindingResult)
        }
        if (bindingResult.hasErrors()) {
            return modelAndView
        }
        val aggregatedDataProfile = AggregatedDataProfile.create(form)
        aggregatedDataProfileRepository.saveAndFlush(aggregatedDataProfile)
        aggregatedDataProfileService.reloadRoutes(aggregatedDataProfile)
        val redirectModelAndView = ModelAndView("$BASE_FRAGMENT_ADG/edit").apply {
            addObject("form", EditAggregatedDataProfileForm.from(aggregatedDataProfile))
            addObject("endpoints", endpoints)
            addObject("relations", aggregatedDataProfile.relations.map { Relation.from(it) })
        }
        return redirectModelAndView
    }

    @GetMapping("/aggregated-data-profiles/edit/{id}")
    fun edit(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val profile = aggregatedDataProfileRepository.getReferenceById(id)
        val form = EditAggregatedDataProfileForm.from(profile)
        val relations = profile.relations.map { Relation.from(it) }
        val viewName = if (isHxRequest) {
            "$BASE_FRAGMENT_ADG/edit"
        } else {
            "$BASE_FRAGMENT_ADG/editPage"
        }
        return ModelAndView(viewName).apply {
            addObject("form", form)
            addObject("endpoints", primaryEndpoints())
            addObject("relations", relations)
            addObject("menuItems", menuItems)
        }
    }

    @PutMapping(path = ["/aggregated-data-profiles"], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun edit(
        @Valid @ModelAttribute form: EditAggregatedDataProfileForm,
        bindingResult: BindingResult
    ): ModelAndView {
        val profile = aggregatedDataProfileRepository.getReferenceById(form.id)
        val modelAndView = ModelAndView("$BASE_FRAGMENT_ADG/edit").apply {
            addObject("errors", bindingResult)
            addObject("form", form)
            addObject("relations", profile.relations.map { Relation.from(it) })
            addObject("endpoints", primaryEndpoints())
        }
        if (bindingResult.hasErrors()) {
            return modelAndView
        }
        profile.handle(form)
        aggregatedDataProfileService.reloadRoutes(profile)
        aggregatedDataProfileRepository.save(profile)
        return modelAndView
    }

    @GetMapping("/aggregated-data-profiles/{id}/relations/create")
    fun relationCreate(@PathVariable id: UUID): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(id)
        val sources = sources(aggregatedDataProfile)
        val endpoints = endpoints()
        val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/add").apply {
            addObject("aggregatedDataProfileId", id)
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
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(form.aggregatedDataProfileId)
        val sources = sources(aggregatedDataProfile)
        val endpoints = endpoints()
        val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/add").apply {
            addObject("aggregatedDataProfileId", form.aggregatedDataProfileId)
            addObject("sources", sources)
            addObject("endpoints", endpoints)
            addObject("errors", bindingResult)
            addObject("form", form)
        }
        if (bindingResult.hasErrors()) {
            return listOf(modelAndView)
        }
        val result = form.run {
            aggregatedDataProfile.let {
                it.addRelation(form)
                aggregatedDataProfileService.reloadRoutes(it)
                aggregatedDataProfileRepository.save(it)
            }
        }
        val relationsModelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/list").apply {
            addObject("relations", result.relations.map { Relation.from(it) })
        }
        return listOf(
            modelAndView,
            relationsModelAndView // OOB Swap
        )
    }

    @GetMapping("/aggregated-data-profiles/{id}/relations/edit/{relationId}")
    fun relationEdit(
        @PathVariable id: UUID,
        @PathVariable relationId: UUID,
    ): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(id)
        val sources = sources(aggregatedDataProfile).apply { this.removeIf { it.id == relationId.toString() } }
        val endpoints = endpoints()
        val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/edit").apply {
            addObject("sources", sources)
            addObject("endpoints", endpoints)
            addObject("form", aggregatedDataProfile.relations.find { it.id == relationId }?.let { EditRelationForm.from(it) })
        }
        return modelAndView
    }

    @PutMapping("/relations")
    fun editRelation(
        @Valid @ModelAttribute form: EditRelationForm,
        bindingResult: BindingResult,
    ): List<ModelAndView> {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(form.aggregatedDataProfileId)
        val sources = sources(aggregatedDataProfile).apply { this.removeIf { it.id == form.id.toString() } }
        val endpoints = endpoints()
        val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/edit").apply {
            addObject("aggregatedDataProfileId", form.aggregatedDataProfileId)
            addObject("sources", sources)
            addObject("endpoints", endpoints)
            addObject("errors", bindingResult)
            addObject("form", form)
        }
        if (bindingResult.hasErrors()) {
            return listOf(modelAndView)
        }
        var updatedProfile: AggregatedDataProfile? = null
        try {
            updatedProfile = form.run {
                aggregatedDataProfile.let {
                    it.changeRelation(form)
                    aggregatedDataProfileService.reloadRoutes(it)
                    aggregatedDataProfileRepository.save(it)
                }
            }
        } catch (ex: DataIntegrityViolationException) {
            bindingResult.addError(ObjectError("name", "A profile with this name already exists."))
            return listOf(modelAndView)
        }
        val relationsModelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/list").apply {
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

    private fun sources(aggregatedDataProfile: AggregatedDataProfile) = aggregatedDataProfile.relations.map { relation ->
        Source(
            id = relation.id.toString(),
            name = relation.id.toString()
        )
    }.toMutableList()

}