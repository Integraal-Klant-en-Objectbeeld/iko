package com.ritense.iko.aggregateddataprofile

import com.ritense.iko.aggregateddataprofile.domain.Transform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TransformTest {
    @Test
    fun `should create valid Transform class`() {
        val transform = Transform("test")
        assertThat(transform.expression).isEqualTo("test")
    }
}