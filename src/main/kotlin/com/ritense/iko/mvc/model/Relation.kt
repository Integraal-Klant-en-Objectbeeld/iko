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

import java.util.UUID
import com.ritense.iko.aggregateddataprofile.domain.Relation as RelationEntity

data class Relation(
    val aggregatedDataProfileId: UUID,
    val id: UUID,
    val sourceId: UUID,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    val propertyName: String,
    val sourceToEndpointMapping: String,
    val resultTransform: String,
) {
    companion object {
        fun from(it: RelationEntity): Relation = Relation(
            aggregatedDataProfileId = it.aggregatedDataProfile.id,
            id = it.id,
            sourceId = it.sourceId ?: it.aggregatedDataProfile.id,
            sourceToEndpointMapping = it.endpointTransform.expression,
            resultTransform = it.resultTransform.expression,
            connectorInstanceId = it.connectorInstanceId,
            connectorEndpointId = it.connectorEndpointId,
            propertyName = it.propertyName,
        )
    }
}