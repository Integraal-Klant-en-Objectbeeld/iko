package com.ritense.iko.profile

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TransformTest {

    @Test
    fun `should create valid Transform class`() {
        val transform = Transform("test")
        assertThat(transform.expression).isEqualTo("test")
    }
}