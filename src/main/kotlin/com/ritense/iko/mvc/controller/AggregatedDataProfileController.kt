/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.mvc.controller

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.aggregateddataprofile.service.AggregatedDataProfileService
import com.ritense.iko.cache.service.CacheService
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_ADP
import com.ritense.iko.mvc.controller.HomeController.Companion.BASE_FRAGMENT_RELATION
import com.ritense.iko.mvc.controller.HomeController.Companion.HX_REQUEST_HEADER
import com.ritense.iko.mvc.controller.HomeController.Companion.PAGE_DEFAULT
import com.ritense.iko.mvc.controller.HomeController.Companion.menuItems
import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.AggregatedDataProfileAddForm
import com.ritense.iko.mvc.model.AggregatedDataProfileEditForm
import com.ritense.iko.mvc.model.CreateVersionForm
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
internal class AggregatedDataProfileController(
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
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(id)
        val instance = connectorInstanceRepository.getReferenceById(aggregatedDataProfile.connectorInstanceId)
        val endpoints = connectorEndpointRepository.findByConnector(instance.connector)
        val availableSources = sources(aggregatedDataProfile)
        val isCached = cacheService.isCached(aggregatedDataProfile.id.toString())
        val versions = aggregatedDataProfileRepository.findVersionsByName(aggregatedDataProfile.name)

        return ModelAndView(
            "$BASE_FRAGMENT_ADP/detail-page" +
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
                "versions" to versions,
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
        val page = aggregatedDataProfileRepository.findAllByIsActiveTrue(pageable)
        val connectorInstancesCount = connectorInstanceRepository.findAll().size
        val endpointsCount = connectorEndpointRepository.findAll().size
        val creationAllowed = connectorInstancesCount > 0 && endpointsCount > 0
        return if (isHxRequest) {
            ModelAndView("$BASE_FRAGMENT_ADP/list").apply {
                addObject("creationAllowed", creationAllowed)
                addObject("aggregatedDataProfiles", page.content)
                addObject("page", page)
                addObject("query", query)
            }
        } else {
            ModelAndView("$BASE_FRAGMENT_ADP/listPage").apply {
                addObject("creationAllowed", creationAllowed)
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
        val connectorInstanceCount = connectorInstanceRepository.findAll().size
        val endpointsCount = connectorEndpointRepository.findAll().size
        val creationAllowed = connectorInstanceCount > 0 && endpointsCount > 0
        val list =
            ModelAndView("$BASE_FRAGMENT_ADP/pagination").apply {
                addObject("aggregatedDataProfiles", page.content)
                addObject("page", page)
                addObject("query", query)
                addObject("creationAllowed", creationAllowed)
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
        val connectorInstanceCount = connectorInstanceRepository.findAll().size
        val endpointsCount = connectorEndpointRepository.findAll().size

        val creationAllowed = connectorInstanceCount > 0 && endpointsCount > 0

        if (isHxRequest) {
            val searchResults =
                ModelAndView("$BASE_FRAGMENT_ADP/filterResults").apply {
                    addObject("aggregatedDataProfiles", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("creationAllowed", creationAllowed)
                    addObject("username", SecurityContextHelper.getUserPropertyByKey("name"))
                    addObject("email", SecurityContextHelper.getUserPropertyByKey("email"))
                }
            val pagination =
                ModelAndView("$BASE_FRAGMENT_ADP/pagination").apply {
                    addObject("aggregatedDataProfiles", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("creationAllowed", creationAllowed)
                    addObject("username", SecurityContextHelper.getUserPropertyByKey("name"))
                    addObject("email", SecurityContextHelper.getUserPropertyByKey("email"))
                }
            return listOf(
                searchResults,
                pagination,
            )
        } else {
            return listOf(
                ModelAndView("$BASE_FRAGMENT_ADP/filterResultsPage").apply {
                    addObject("aggregatedDataProfiles", page.content)
                    addObject("page", page)
                    addObject("query", query)
                    addObject("creationAllowed", creationAllowed)
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
        val modelAndView = ModelAndView("$BASE_FRAGMENT_ADP/add").apply {
            addObject("connectorInstances", connectors)
            addObject("connectorEndpoints", connectorEndpointRepository.findByConnector(connectors.first().connector))
        }
        return modelAndView
    }

    @GetMapping("/aggregated-data-profiles/create/endpoints")
    fun endpoints(
        @RequestParam connectorInstanceId: UUID,
    ): ModelAndView {
        val connector = connectorInstanceRepository.getReferenceById(connectorInstanceId)
        val endpoints = connectorEndpointRepository.findByConnector(connector.connector)
        return ModelAndView("$BASE_FRAGMENT_ADP/add :: connectorEndpoints").apply {
            addObject("connectorEndpoints", endpoints)
        }
    }

    @GetMapping("/aggregated-data-profiles/relations/add/endpoints")
    fun relationAddEndpoints(
        @RequestParam connectorInstanceId: UUID,
    ): ModelAndView {
        val connector = connectorInstanceRepository.getReferenceById(connectorInstanceId)
        val endpoints = connectorEndpointRepository.findByConnector(connector.connector)
        return ModelAndView("$BASE_FRAGMENT_RELATION/add :: connectorEndpoints").apply {
            addObject("connectorEndpoints", endpoints)
        }
    }

    @GetMapping("/aggregated-data-profiles/relations/edit/endpoints")
    fun relationEditEndpoints(
        @RequestParam connectorInstanceId: UUID,
    ): ModelAndView {
        val connector = connectorInstanceRepository.getReferenceById(connectorInstanceId)
        val endpoints = connectorEndpointRepository.findByConnector(connector.connector)
        return ModelAndView("$BASE_FRAGMENT_RELATION/edit :: connectorEndpoints").apply {
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
            val modelAndView = ModelAndView("$BASE_FRAGMENT_ADP/add :: adp-add-form").apply {
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
        aggregatedDataProfile.isActive = true
        aggregatedDataProfileRepository.saveAndFlush(aggregatedDataProfile)
        aggregatedDataProfileService.loadRoute(aggregatedDataProfile)

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
            val modelAndView = ModelAndView("$BASE_FRAGMENT_ADP/edit :: profile-edit").apply {
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
        aggregatedDataProfileRepository.save(aggregatedDataProfile)
        // Routes are not reloaded here - changes only apply when the version is activated

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
        aggregatedDataProfileRepository.save(aggregatedDataProfile)
        if (aggregatedDataProfile.isActive) {
            aggregatedDataProfileService.reloadRoute(aggregatedDataProfile)
        }

        val relationsModelAndView = ModelAndView("$BASE_FRAGMENT_ADP/relations-panel :: relations-panel").apply {
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
        val isCached = cacheService.isCached(form.id.toString())

        val modelAndView = ModelAndView("$BASE_FRAGMENT_RELATION/edit :: relation-edit").apply {
            addObject("aggregatedDataProfileId", form.aggregatedDataProfileId)
            addObject("sources", sources)
            addObject("errors", bindingResult)
            addObject("form", form)
            addObject("connectorInstances", connectorInstanceRepository.findAll())
            addObject("connectorEndpoints", connectorEndpoints)
            addObject("isCached", isCached)
        }
        if (bindingResult.hasErrors()) {
            return listOf(modelAndView)
        }
        aggregatedDataProfile.changeRelation(form)
        aggregatedDataProfileRepository.save(aggregatedDataProfile)
        if (aggregatedDataProfile.isActive) {
            aggregatedDataProfileService.reloadRoute(aggregatedDataProfile)
        }

        val refreshedTree = ModelAndView("$BASE_FRAGMENT_ADP/relations-panel :: relations-panel").apply {
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
        aggregatedDataProfile.removeRelation(form)
        aggregatedDataProfileRepository.save(aggregatedDataProfile)
        if (aggregatedDataProfile.isActive) {
            aggregatedDataProfileService.reloadRoute(aggregatedDataProfile)
        }

        val modelAndView = ModelAndView("$BASE_FRAGMENT_ADP/relations-panel :: relations-panel").apply {
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
        aggregatedDataProfileService.removeRoute(aggregatedDataProfile)
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

    @DeleteMapping("/aggregated-data-profiles/{id}/relation/{relationId}/cache")
    fun evictRelationCacheKey(
        @PathVariable id: UUID,
        @PathVariable relationId: UUID,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(id)
        val relation = aggregatedDataProfile.relations.first { it.id == relationId }
        cacheService.evictByPrefix(relation.id.toString())
        httpServletResponse.setHeader("HX-Retarget", "#view-panel")
        httpServletResponse.setHeader("HX-Reswap", "innerHTML")
        return details(id, true)
    }

    @GetMapping("/aggregated-data-profiles/{id}/versions/create")
    fun createVersionModal(
        @PathVariable id: UUID,
    ): ModelAndView {
        val profile = aggregatedDataProfileRepository.findById(id).orElseThrow()
        return ModelAndView("fragments/internal/versioning/create-version-modal :: create-version-modal").apply {
            addObject("entityType", "aggregated-data-profile")
            addObject("entityId", id)
            addObject("entityName", profile.name)
            addObject("currentVersion", profile.version.value)
            addObject("form", CreateVersionForm())
        }
    }

    @PostMapping("/aggregated-data-profiles/{id}/versions")
    fun createVersion(
        @PathVariable id: UUID,
        @Valid @ModelAttribute form: CreateVersionForm,
        bindingResult: BindingResult,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        val profile = aggregatedDataProfileRepository.findById(id).orElseThrow()

        if (bindingResult.hasErrors()) {
            return ModelAndView("fragments/internal/versioning/create-version-modal :: form").apply {
                addObject("entityType", "aggregated-data-profile")
                addObject("entityId", id)
                addObject("entityName", profile.name)
                addObject("currentVersion", profile.version.value)
                addObject("form", form)
                addObject("errors", bindingResult)
            }
        }

        return try {
            val newProfile = aggregatedDataProfileService.createNewVersion(id, form.version)

            httpServletResponse.setHeader("HX-Trigger", "close-modal")
            httpServletResponse.setHeader("HX-Push-Url", "/admin/aggregated-data-profiles/${newProfile.id}")
            httpServletResponse.setHeader("HX-Retarget", "#view-panel")
            httpServletResponse.setHeader("HX-Reswap", "innerHTML")

            details(newProfile.id, isHxRequest)
        } catch (e: IllegalArgumentException) {
            bindingResult.rejectValue("version", "duplicate", e.message ?: "Version already exists")
            ModelAndView("fragments/internal/versioning/create-version-modal :: form").apply {
                addObject("entityType", "aggregated-data-profile")
                addObject("entityId", id)
                addObject("entityName", profile.name)
                addObject("currentVersion", profile.version.value)
                addObject("form", form)
                addObject("errors", bindingResult)
            }
        }
    }

    @PostMapping("/aggregated-data-profiles/{id}/activate")
    fun activateVersion(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        aggregatedDataProfileService.activateVersion(id)
        return details(id, isHxRequest)
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