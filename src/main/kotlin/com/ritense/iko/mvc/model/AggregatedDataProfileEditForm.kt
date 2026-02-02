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

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.Version
import com.ritense.iko.camel.IkoConstants.Validation.ROLES_PATTERN
import com.ritense.iko.mvc.model.validation.UniqueAggregatedDataProfile
import com.ritense.iko.mvc.model.validation.ValidTransform
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.util.UUID

data class AggregatedDataProfileEditForm(
    override val id: UUID,
    @field:NotBlank(message = "Please provide a name.")
    @field:Pattern(
        regexp = "[0-9a-zA-Z_\\-]+",
        message = "Name may only contain letters, digits, underscores, and hyphens.",
    )
    override val name: String,
    @field:NotBlank(message = "Please provide roles.")
    @field:Pattern(
        regexp = ROLES_PATTERN,
        message = "Roles must be a comma-separated list of values (e.g., ROLE_ADMIN,ROLE_USER).",
    )
    val roles: String,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    @field:ValidTransform
    @field:NotBlank(message = "Please provide a transform expression.")
    val endpointTransform: String,
    @field:ValidTransform
    @field:NotBlank(message = "Please provide a transform expression.")
    val resultTransform: String,
    val cacheEnabled: Boolean,
    @field:Min(value = 0)
    val cacheTimeToLive: Int,
    val version: Version,
): UniqueAggregatedDataProfile {

    companion object {
        fun from(aggregatedDataProfile: AggregatedDataProfile) = AggregatedDataProfileEditForm(
            id = aggregatedDataProfile.id,
            name = aggregatedDataProfile.name,
            roles = aggregatedDataProfile.roles.value,
            connectorInstanceId = aggregatedDataProfile.connectorInstanceId,
            connectorEndpointId = aggregatedDataProfile.connectorEndpointId,
            endpointTransform = aggregatedDataProfile.endpointTransform.expression,
            resultTransform = aggregatedDataProfile.resultTransform.expression,
            cacheEnabled = aggregatedDataProfile.aggregatedDataProfileCacheSetting.enabled,
            cacheTimeToLive = aggregatedDataProfile.aggregatedDataProfileCacheSetting.timeToLive,
            version = aggregatedDataProfile.version,
        )
    }
}