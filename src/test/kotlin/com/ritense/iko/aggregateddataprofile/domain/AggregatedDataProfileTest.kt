package com.ritense.iko.aggregateddataprofile.domain

import com.ritense.iko.mvc.model.AggregatedDataProfileAddForm
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class AggregatedDataProfileTest {

    @Test
    fun `create builds aggregated data profile from valid form`() {
        val form = AggregatedDataProfileAddForm(
            name = "pets",
            role = "ROLE_ADMIN",
            endpointTransform = ".",
            resultTransform = ".",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
        )

        val profile = AggregatedDataProfile.create(form)

        assertThat(profile.name).isEqualTo("pets")
        assertThat(profile.role).isEqualTo("ROLE_ADMIN")
        assertThat(profile.endpointTransform.expression).isEqualTo(".")
        assertThat(profile.resultTransform.expression).isEqualTo(".")
        assertThat(profile.connectorInstanceId).isEqualTo(form.connectorInstanceId)
        assertThat(profile.connectorEndpointId).isEqualTo(form.connectorEndpointId)
    }

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
    fun `create throws when endpoint transform is invalid`() {
        val form = AggregatedDataProfileAddForm(
            name = "pets",
            role = "ROLE_ADMIN",
            endpointTransform = "?",
            resultTransform = ".",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
        )

        assertThatThrownBy { AggregatedDataProfile.create(form) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid expression")
    }

    @Test
    fun `create throws when result transform is invalid`() {
        val form = AggregatedDataProfileAddForm(
            name = "pets",
            role = "ROLE_ADMIN",
            endpointTransform = ".",
            resultTransform = "?",
            connectorInstanceId = UUID.randomUUID(),
            connectorEndpointId = UUID.randomUUID(),
        )

        assertThatThrownBy { AggregatedDataProfile.create(form) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid expression")
    }
}