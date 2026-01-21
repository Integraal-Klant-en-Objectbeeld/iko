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
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@UniqueRelationCheck
data class AddRelationForm(
    override val aggregatedDataProfileId: UUID,
    override val sourceId: UUID,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    @field:NotBlank(message = "Please provide a mapping.")
    val sourceToEndpointMapping: String,
    @field:NotBlank(message = "Please define a transform expression.")
    @field:ValidTransform
    val resultTransform: String,
    @field:NotBlank(message = "Please provide a property name.")
    override val propertyName: String,
    override val id: UUID? = null,
) : UniqueRelation