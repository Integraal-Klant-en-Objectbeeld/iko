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
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfileCacheSetting
import com.ritense.iko.aggregateddataprofile.domain.EndpointTransform
import com.ritense.iko.aggregateddataprofile.domain.Roles
import com.ritense.iko.aggregateddataprofile.domain.Transform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class AggregatedDataProfileFormTest {

    @Test
    fun `add form can be created from aggregated data profile`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(),
            roles = Roles("ROLE_ADMIN"),
        )

        val form = AggregatedDataProfileAddForm.from(profile)

        assertThat(form.name).isEqualTo("pets")
        assertThat(form.roles).isEqualTo("ROLE_ADMIN")
        assertThat(form.endpointTransform).isEqualTo(".")
        assertThat(form.resultTransform).isEqualTo(".")
        assertThat(form.connectorInstanceId).isEqualTo(profile.connectorInstanceId)
        assertThat(form.connectorEndpointId).isEqualTo(profile.connectorEndpointId)
    }

    @Test
    fun `edit form can be created from aggregated data profile without name`() {
        val profile = AggregatedDataProfile(
            id = UUID.randomUUID(),
            name = "pets",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
            endpointTransform = EndpointTransform("."),
            resultTransform = Transform("."),
            aggregatedDataProfileCacheSetting = AggregatedDataProfileCacheSetting(
                enabled = true,
                timeToLive = 123,
            ),
            roles = Roles("ROLE_ADMIN"),
        )

        val form = AggregatedDataProfileEditForm.from(profile)

        assertThat(form.id).isEqualTo(profile.id)
        // Note: name is not included in edit form (immutable after creation)
        assertThat(form.roles).isEqualTo("ROLE_ADMIN")
        assertThat(form.endpointTransform).isEqualTo(".")
        assertThat(form.resultTransform).isEqualTo(".")
        assertThat(form.connectorInstanceId).isEqualTo(profile.connectorInstanceId)
        assertThat(form.connectorEndpointId).isEqualTo(profile.connectorEndpointId)
        assertThat(form.cacheEnabled).isTrue()
        assertThat(form.cacheTimeToLive).isEqualTo(123)
    }
}