package com.ritense.iko.mvc.controller

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.aggregateddataprofile.service.AggregatedDataProfileService
import com.ritense.iko.cache.service.CacheService
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_ADG
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_RELATION
import com.ritense.iko.mvc.controller.HomeController.Companion.HX_REQUEST_HEADER
import com.ritense.iko.mvc.controller.HomeController.Companion.PAGE_DEFAULT
import com.ritense.iko.mvc.controller.HomeController.Companion.menuItems
import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.AggregatedDataProfileAddForm
import com.ritense.iko.mvc.model.AggregatedDataProfileEditForm
import com.ritense.iko.mvc.model.DeleteRelationForm
import com.ritense.iko.mvc.model.EditRelationForm
import com.ritense.iko.mvc.model.Relation
import com.ritense.iko.mvc.model.Source
import com.ritense.iko.security.SecurityContextHelper
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.DeleteMapping
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
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val cacheService: CacheService,
) {
    @GetMapping("/aggregated-data-profiles/{id}")
    fun details(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        val aggregatedDataProfile =
            aggregatedDataProfileRepository.findById(id).orElseThrow { NoSuchElementException("ADP not found") }
        val instance = connectorInstanceRepository.findById(aggregatedDataProfile.connectorInstanceId).orElse(null)
        val endpoints = instance?.let { connectorEndpointRepository.findByConnector(it.connector) } ?: emptyList()
        val availableSources = sources(aggregatedDataProfile)
        val isCached = cacheService.isCached(aggregatedDataProfile.id.toString())

        return ModelAndView(
            "$BASE_FRAGMENT_ADG/detailPage" +
                when (isHxRequest) {
                    true -> ":: view-panel-content"
                    false -> ""
                },
            mapOf(
                "aggregatedDataProfile" to aggregatedDataProfile,
                "form" to AggregatedDataProfileEditForm.from(aggregatedDataProfile),
                "relations" to aggregatedDataProfile.relations.map { Relation.from(it) },
                "aggregatedDataProfileId" to aggregatedDataProfile.id,
                "connectorInstances" to connectorInstanceRepository.findAll(),
                "connectorEndpoints" to endpoints,
                "sources" to availableSources,
                "isCached" to isCached,
                "username" to SecurityContextHelper.getUserPropertyByKey("name"),
                "email" to SecurityContextHelper.getUserPropertyByKey("email"),
            ),
        )
    }

    @GetMapping("/aggregated-data-profiles")
    fun list(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
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
                addObject("username", SecurityContextHelper.getUserPropertyByKey("name"))
                addObject("email", SecurityContextHelper.getUserPropertyByKey("email"))
            }
        }
    }

    @GetMapping("/aggregated-data-profiles/pagination")
    fun pagination(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
    ): ModelAndView {
        val page = aggregatedDataProfileRepository.findAll(pageable)
        val list =
            ModelAndView("$BASE_FRAGMENT_ADG/pagination").apply {
                addObject("aggregatedDataProfiles", page.content)
                addObject("page", page)
                addObject("query", query)
                addObject("username", SecurityContextHelper.getUserPropertyByKey("name"))
                addObject("email", SecurityContextHelper.getUserPropertyByKey("email"))
            }
        return list
    }

    @GetMapping("/aggregated-data-profiles/filter")
    fun filter(
        @RequestParam(required = false, defaultValue = "") query: String,
        @PageableDefault(size = PAGE_DEFAULT) pageable: Pageable,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): List<ModelAndView> {
        val page =
            if (query.isBlank()) {
                aggregatedDataProfileRepository.findAllBy(pageable)
            } else {
                aggregatedDataProfileRepository.findAllByName(query.trim(), pageable)
            }

        if (isHxRequest) {
            val searchResults =
                ModelAndView("$BASE_FRAGMENT_ADG/filterResults").apply {
                    addObject("aggregatedDataProfiles", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("username", SecurityContextHelper.getUserPropertyByKey("name"))
                    addObject("email", SecurityContextHelper.getUserPropertyByKey("email"))
                }
            val pagination =
                ModelAndView("$BASE_FRAGMENT_ADG/pagination").apply {
                    addObject("aggregatedDataProfiles", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("username", SecurityContextHelper.getUserPropertyByKey("name"))
                    addObject("email", SecurityContextHelper.getUserPropertyByKey("email"))
                }
            return listOf(
                searchResults,
                pagination,
            )
        } else {
            return listOf(
                ModelAndView("$BASE_FRAGMENT_ADG/filterResultsPage").apply {
                    addObject("aggregatedDataProfiles", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("menuItems", menuItems)
                    addObject("username", SecurityContextHelper.getUserPropertyByKey("name"))
                    addObject("email", SecurityContextHelper.getUserPropertyByKey("email"))
                },
            )
        }
    }

    @GetMapping("/aggregated-data-profiles/create")
    fun create(): ModelAndView {
        val connectors = connectorInstanceRepository.findAll()
        val modelAndView = ModelAndView("$BASE_FRAGMENT_ADG/add").apply {
            addObject("connectorInstances", connectors)
            addObject("connectorEndpoints", connectorEndpointRepository.findByConnector(connectors.first().connector))
        }
        return modelAndView
    }

    @GetMapping("/aggregated-data-profiles/create/endpoints")
    fun endpoints(
        @RequestParam connectorInstanceId: UUID,
    ): ModelAndView {
        val connector = connectorInstanceRepository
            .findById(connectorInstanceId)
            .orElseThrow { NoSuchElementException("Connector not found") }
        val endpoints = connectorEndpointRepository.findByConnector(connector.connector)

        return ModelAndView("$BASE_FRAGMENT_ADG/add :: connectorEndpoints").apply {
            addObject("connectorEndpoints", endpoints)
        }
    }

    @PostMapping(
        path = ["/aggregated-data-profiles"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    @Transactional
    fun create(
        @Valid @ModelAttribute form: AggregatedDataProfileAddForm,
        bindingResult: BindingResult,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        if (bindingResult.hasErrors()) {
            val modelAndView = ModelAndView("$BASE_FRAGMENT_ADG/add :: adp-add-form").apply {
                addObject("form", form)
                addObject("errors", bindingResult)
                addObject("connectorInstances", connectorInstanceRepository.findAll())
                addObject(
                    "connectorEndpoints",
                    connectorInstanceRepository.findById(form.connectorInstanceId)
                        .map { connectorEndpointRepository.findByConnector(it.connector) }.orElseThrow(),
                )
            }
            return modelAndView
        }
        val aggregatedDataProfile = AggregatedDataProfile.create(form)
        aggregatedDataProfileRepository.saveAndFlush(aggregatedDataProfile)
        aggregatedDataProfileService.reloadRoutes(aggregatedDataProfile)

        httpServletResponse.setHeader("HX-Trigger", "close-modal")
        httpServletResponse.setHeader("HX-Push-Url", "/admin/aggregated-data-profiles/${aggregatedDataProfile.id}")
        httpServletResponse.setHeader("HX-Retarget", "#view-panel")
        httpServletResponse.setHeader("HX-Reswap", "innerHTML")

        return details(aggregatedDataProfile.id, isHxRequest = true)
    }

    @PutMapping(
        path = ["/aggregated-data-profiles"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    fun edit(
        @Valid @ModelAttribute form: AggregatedDataProfileEditForm,
        bindingResult: BindingResult,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(form.id)
        val isCached = cacheService.isCached(aggregatedDataProfile.id.toString())
        val instance = connectorInstanceRepository.findById(aggregatedDataProfile.connectorInstanceId).orElseThrow()
        if (bindingResult.hasErrors()) {
            val modelAndView = ModelAndView("$BASE_FRAGMENT_ADG/edit :: profile-edit").apply {
                addObject("errors", bindingResult)
                addObject("form", form)
                addObject("relations", aggregatedDataProfile.relations.map { Relation.from(it) })
                addObject("connectorInstances", connectorInstanceRepository.findAll())
                addObject("connectorEndpoints", connectorEndpointRepository.findByConnector(instance.connector))
                addObject("isCached", isCached)
            }
            return modelAndView
        }
        aggregatedDataProfile.handle(form)
        aggregatedDataProfileService.reloadRoutes(aggregatedDataProfile)
        aggregatedDataProfileRepository.save(aggregatedDataProfile)

        httpServletResponse.setHeader("HX-Push-Url", "/admin/aggregated-data-profiles/${aggregatedDataProfile.id}")
        httpServletResponse.setHeader("HX-Retarget", "#view-panel")
        httpServletResponse.setHeader("HX-Reswap", "innerHTML")

        return details(aggregatedDataProfile.id, isHxRequest = true)
    }

    @GetMapping("/aggregated-data-profiles/{id}/relations/create")
    fun relationCreate(
        @PathVariable id: UUID,
        @RequestParam(required = false) sourceId: String? = null,
    ): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(id)
        val sources = sources(aggregatedDataProfile)
        val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/add").apply {
            addObject("aggregatedDataProfileId", id)
            addObject("connectorInstances", connectorInstanceRepository.findAll())
            addObject("connectorEndpoints", connectorEndpointRepository.findAll())
            addObject("sources", sources)
            addObject("parentId", sourceId)
        }
        return modelAndView
    }

    @PostMapping("/relations")
    fun createRelation(
        @Valid @ModelAttribute form: AddRelationForm,
        bindingResult: BindingResult,
        httpServletResponse: HttpServletResponse,
    ): List<ModelAndView> {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(form.aggregatedDataProfileId)
        val sources = sources(aggregatedDataProfile)
        val connectorInstance =
            connectorInstanceRepository.findById(aggregatedDataProfile.connectorInstanceId).orElse(null)
        val connectorEndpoints =
            connectorInstance?.let { connectorEndpointRepository.findByConnector(it.connector) } ?: emptyList()

        if (bindingResult.hasErrors()) {
            val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/add :: relation-add").apply {
                addObject("aggregatedDataProfileId", form.aggregatedDataProfileId)
                addObject("sources", sources)
                addObject("errors", bindingResult)
                addObject("form", form)
                addObject("connectorInstances", connectorInstanceRepository.findAll())
                addObject("connectorEndpoints", connectorEndpoints)
            }

            return listOf(modelAndView)
        }

        aggregatedDataProfile.addRelation(form)
        aggregatedDataProfileService.reloadRoutes(aggregatedDataProfile)
        aggregatedDataProfileRepository.save(aggregatedDataProfile)

        val relationsModelAndView = ModelAndView("$BASE_FRAGMENT_ADG/relationsPanel :: relations-panel").apply {
            addObject("aggregatedDataProfile", aggregatedDataProfile)
            addObject("form", AggregatedDataProfileEditForm.from(aggregatedDataProfile))
            addObject("relations", aggregatedDataProfile.relations.map { Relation.from(it) })
            addObject("sources", sources)
            addObject("aggregatedDataProfileId", aggregatedDataProfile.id)
            addObject("connectorInstances", connectorInstanceRepository.findAll())
            addObject("connectorEndpoints", connectorEndpointRepository.findAll())
        }

        httpServletResponse.setHeader("HX-Push-Url", "/admin/aggregated-data-profiles/${aggregatedDataProfile.id}")
        httpServletResponse.setHeader("HX-Retarget", "#panel-relations")
        httpServletResponse.setHeader("HX-Reswap", "innerHTML")

        return listOf(relationsModelAndView)
    }

    @GetMapping("/aggregated-data-profiles/{id}/relations/edit/{relationId}")
    fun relationEdit(
        @PathVariable id: UUID,
        @PathVariable relationId: UUID,
    ): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(id)
        val relation = aggregatedDataProfile.relations.find { it.id == relationId }
        val connector = connectorInstanceRepository.findById(relation?.connectorInstanceId!!).orElseThrow()
        val sources = sources(aggregatedDataProfile).apply { this.removeIf { it.id == relationId.toString() } }
        val isCached = cacheService.isCached(relation.id.toString())
        val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/edit").apply {
            addObject("sources", sources)
            addObject("connectorInstances", connectorInstanceRepository.findAll())
            addObject("connectorEndpoints", connectorEndpointRepository.findByConnector(connector.connector))
            addObject("form", EditRelationForm.from(relation))
            addObject("isCached", isCached)
        }
        return modelAndView
    }

    @PutMapping("/relations")
    fun editRelation(
        @Valid @ModelAttribute form: EditRelationForm,
        bindingResult: BindingResult,
        httpServletResponse: HttpServletResponse,
    ): List<ModelAndView> {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(form.aggregatedDataProfileId)
        val sources = sources(aggregatedDataProfile).apply { this.removeIf { it.id == form.id.toString() } }
        val connectorInstance = connectorInstanceRepository.findById(form.connectorInstanceId).orElse(null)
        val connectorEndpoints =
            connectorInstance?.let { connectorEndpointRepository.findByConnector(it.connector) } ?: emptyList()
        val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/edit :: relation-edit").apply {
            addObject("aggregatedDataProfileId", form.aggregatedDataProfileId)
            addObject("sources", sources)
            addObject("errors", bindingResult)
            addObject("form", form)
            addObject("connectorInstances", connectorInstanceRepository.findAll())
            addObject("connectorEndpoints", connectorEndpoints)
        }
        if (bindingResult.hasErrors()) {
            return listOf(modelAndView)
        }
        aggregatedDataProfile.changeRelation(form)
        aggregatedDataProfileService.reloadRoutes(aggregatedDataProfile)
        aggregatedDataProfileRepository.save(aggregatedDataProfile)

        val refreshedTree = ModelAndView("$BASE_FRAGMENT_ADG/relationsPanel :: relations-panel").apply {
            addObject("aggregatedDataProfile", aggregatedDataProfile)
            addObject("relations", aggregatedDataProfile.relations.map { Relation.from(it) })
        }

        httpServletResponse.setHeader("HX-Push-Url", "/admin/aggregated-data-profiles/${aggregatedDataProfile.id}")
        httpServletResponse.setHeader("HX-Retarget", "#panel-relations")
        httpServletResponse.setHeader("HX-Reswap", "innerHTML")

        return listOf(refreshedTree)
    }

    @GetMapping("/aggregated-data-profiles/{id}/relations/edit/{relationId}/delete")
    fun relationDelete(
        @PathVariable id: UUID,
        @PathVariable relationId: UUID,
    ): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(id)
        val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/delete").apply {
            addObject(
                "form",
                aggregatedDataProfile.relations.find { it.id == relationId }?.let { EditRelationForm.from(it) },
            )
        }
        return modelAndView
    }

    @DeleteMapping("/relations")
    fun deleteRelation(
        @Valid @ModelAttribute form: DeleteRelationForm,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(form.aggregatedDataProfileId)
        form.run {
            aggregatedDataProfile.let {
                it.removeRelation(form)
                aggregatedDataProfileService.reloadRoutes(it)
                aggregatedDataProfileRepository.save(it)
            }
        }
        val modelAndView = ModelAndView("$BASE_FRAGMENT_ADG/relationsPanel :: relations-panel").apply {
            addObject("aggregatedDataProfile", aggregatedDataProfile)
            addObject("form", AggregatedDataProfileEditForm.from(aggregatedDataProfile))
            addObject("relations", aggregatedDataProfile.relations.map { Relation.from(it) })
        }

        httpServletResponse.setHeader("HX-Push-Url", "/admin/aggregated-data-profiles/${aggregatedDataProfile.id}")
        httpServletResponse.setHeader("HX-Retarget", "#panel-relations")
        httpServletResponse.setHeader("HX-Reswap", "innerHTML")
        httpServletResponse.setHeader("HX-Trigger", "close-modal")

        return modelAndView
    }

    @DeleteMapping("/aggregated-data-profiles/{id}")
    @Transactional
    fun deleteAggregatedDataProfile(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(id)
        aggregatedDataProfileService.removeRoutes(aggregatedDataProfile)
        aggregatedDataProfileRepository.delete(aggregatedDataProfile)

        httpServletResponse.setHeader("HX-Push-Url", "/admin/aggregated-data-profiles")
        httpServletResponse.setHeader("HX-Retarget", "#view-panel")
        httpServletResponse.setHeader("HX-Reswap", "innerHTML")

        return list(query = "", pageable = Pageable.ofSize(PAGE_DEFAULT), isHxRequest = isHxRequest)
    }

    @DeleteMapping("/aggregated-data-profiles/{id}/cache")
    fun evictAggregatedDataProfileCacheKey(
        @PathVariable id: UUID,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(id)
        cacheService.evictByPrefix(aggregatedDataProfile.id.toString())
        httpServletResponse.setHeader("HX-Retarget", "#view-panel")
        httpServletResponse.setHeader("HX-Reswap", "innerHTML")
        return details(id, true)
    }

    private fun sources(aggregatedDataProfile: AggregatedDataProfile) = aggregatedDataProfile.relations
        .sortedWith(
            compareBy<com.ritense.iko.aggregateddataprofile.domain.Relation>(
                // first criterion: is this relation's sourceId equal to aggregatedDataProfile.id?
                { it.sourceId != aggregatedDataProfile.id }, // false (0) comes before true (1)
            ).thenBy { it.sourceId }, // second criterion: normal ascending by parentId
        ).map { relation ->
            Source(
                id = relation.id.toString(),
                name =
                    if (relation.sourceId == aggregatedDataProfile.id) {
                        aggregatedDataProfile.name + ">" + relation.propertyName // use profile name if sourceId matches
                    } else {
                        relation.propertyName // otherwise use relation id
                    },
            )
        }
        .toMutableList()
}