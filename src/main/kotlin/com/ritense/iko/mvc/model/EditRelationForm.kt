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

package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.UniqueRelation
import com.ritense.iko.mvc.model.validation.UniqueRelationCheck
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import com.ritense.iko.aggregateddataprofile.domain.Relation as RelationEntity

@UniqueRelationCheck
data class EditRelationForm(
    override val aggregatedDataProfileId: UUID,
    override val id: UUID,
    override val sourceId: UUID,
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToEndpointMapping: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val resultTransform: String,
    @field:NotBlank(message = "Please provide a property name.")
    override val propertyName: String,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    val cacheEnabled: Boolean,
    @field:Min(value = 0)
    val cacheTimeToLive: Int,
) : UniqueRelation {
    companion object {
        fun from(it: RelationEntity): EditRelationForm = EditRelationForm(
            aggregatedDataProfileId = it.aggregatedDataProfile.id,
            id = it.id,
            sourceId = it.sourceId ?: it.aggregatedDataProfile.id,
            sourceToEndpointMapping = it.endpointTransform.expression,
            resultTransform = it.resultTransform.expression,
            connectorInstanceId = it.connectorInstanceId,
            connectorEndpointId = it.connectorEndpointId,
            propertyName = it.propertyName,
            cacheEnabled = it.relationCacheSettings.enabled,
            cacheTimeToLive = it.relationCacheSettings.timeToLive,
        )
    }
}