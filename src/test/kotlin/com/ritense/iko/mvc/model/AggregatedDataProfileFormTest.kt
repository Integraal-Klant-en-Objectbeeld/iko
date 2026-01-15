package com.ritense.iko.mvc.model

import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfileCacheSetting
import com.ritense.iko.aggregateddataprofile.domain.EndpointTransform
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
            role = "ROLE_ADMIN",
        )

        val form = AggregatedDataProfileAddForm.from(profile)

        assertThat(form.name).isEqualTo("pets")
        assertThat(form.role).isEqualTo("ROLE_ADMIN")
        assertThat(form.endpointTransform).isEqualTo(".")
        assertThat(form.resultTransform).isEqualTo(".")
        assertThat(form.connectorInstanceId).isEqualTo(profile.connectorInstanceId)
        assertThat(form.connectorEndpointId).isEqualTo(profile.connectorEndpointId)
    }

    @Test
    fun `edit form can be created from aggregated data profile`() {
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
            role = "ROLE_ADMIN",
        )

        val form = AggregatedDataProfileEditForm.from(profile)

        assertThat(form.id).isEqualTo(profile.id)
        assertThat(form.name).isEqualTo("pets")
        assertThat(form.role).isEqualTo("ROLE_ADMIN")
        assertThat(form.endpointTransform).isEqualTo(".")
        assertThat(form.resultTransform).isEqualTo(".")
        assertThat(form.connectorInstanceId).isEqualTo(profile.connectorInstanceId)
        assertThat(form.connectorEndpointId).isEqualTo(profile.connectorEndpointId)
        assertThat(form.cacheEnabled).isTrue()
        assertThat(form.cacheTimeToLive).isEqualTo(123)
    }
}