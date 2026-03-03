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
import com.ritense.iko.mvc.model.AddRelationForm
import com.ritense.iko.mvc.model.AggregatedDataProfileEditForm
import com.ritense.iko.mvc.model.DeleteRelationForm
import com.ritense.iko.mvc.model.EditRelationForm
import com.ritense.iko.mvc.model.Relation
import com.ritense.iko.mvc.model.Source
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

@Controller("mvcRelationController")
@RequestMapping("/admin/relations")
internal class RelationController(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val aggregatedDataProfileService: AggregatedDataProfileService,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val cacheService: CacheService,
) {

    @PostMapping
    fun createRelation(
        @Valid @ModelAttribute form: AddRelationForm,
        bindingResult: org.springframework.validation.BindingResult,
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

    @PutMapping
    fun editRelation(
        @Valid @ModelAttribute form: EditRelationForm,
        bindingResult: org.springframework.validation.BindingResult,
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

    @DeleteMapping
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

    private fun sources(aggregatedDataProfile: AggregatedDataProfile) = aggregatedDataProfile.relations
        .sortedWith(
            compareBy<com.ritense.iko.aggregateddataprofile.domain.Relation>(
                { it.sourceId != aggregatedDataProfile.id },
            ).thenBy { it.sourceId },
        ).map { relation ->
            Source(
                id = relation.id.toString(),
                name =
                if (relation.sourceId == aggregatedDataProfile.id) {
                    aggregatedDataProfile.name + ">" + relation.propertyName
                } else {
                    relation.propertyName
                },
            )
        }
        .toMutableList()
}