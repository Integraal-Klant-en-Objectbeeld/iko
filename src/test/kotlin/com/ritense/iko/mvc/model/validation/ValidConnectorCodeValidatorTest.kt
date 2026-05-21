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

package com.ritense.iko.mvc.model.validation

import jakarta.validation.ConstraintValidatorContext
import org.apache.camel.impl.DefaultCamelContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ValidConnectorCodeValidatorTest {
    private lateinit var camelContext: DefaultCamelContext
    private lateinit var validator: ValidConnectorCodeValidator
    private val context: ConstraintValidatorContext = mock()
    private val violationBuilder: ConstraintValidatorContext.ConstraintViolationBuilder = mock()

    @BeforeEach
    fun setup() {
        camelContext = DefaultCamelContext()
        camelContext.start()
        validator = ValidConnectorCodeValidator(camelContext)
        whenever(context.buildConstraintViolationWithTemplate(org.mockito.kotlin.any()))
            .thenReturn(violationBuilder)
    }

    @AfterEach
    fun teardown() {
        camelContext.stop()
    }

    @Test
    fun `blank connector code is valid (handled by NotBlank)`() {
        assertThat(validator.isValid(null, context)).isTrue()
        assertThat(validator.isValid("", context)).isTrue()
        assertThat(validator.isValid("   ", context)).isTrue()
    }

    @Test
    fun `connector code with only a main route is valid`() {
        val yaml = yamlRoute("direct:iko:connector:foo")
        assertThat(validator.isValid(yaml, context)).isTrue()
    }

    @Test
    fun `connector code with main route plus auxiliary suffix route is valid`() {
        val yaml = """
            - route:
                id: "route-main"
                from:
                  uri: "direct:iko:connector:foo"
                  steps:
                    - log: "main"
            - route:
                id: "route-aux"
                from:
                  uri: "direct:iko:connector:foo:auth"
                  steps:
                    - log: "aux"
        """.trimIndent()
        assertThat(validator.isValid(yaml, context)).isTrue()
    }

    @Test
    fun `connector code with only an auxiliary route (no main) is invalid`() {
        val yaml = yamlRoute("direct:iko:connector:foo:auth")
        assertThat(validator.isValid(yaml, context)).isFalse()
    }

    @Test
    fun `connector route URI with dot in suffix is invalid`() {
        val yaml = """
            - route:
                id: "route-main"
                from:
                  uri: "direct:iko:connector:foo"
                  steps:
                    - log: "main"
            - route:
                id: "route-bad"
                from:
                  uri: "direct:iko:connector:foo:bad.dot"
                  steps:
                    - log: "bad"
        """.trimIndent()
        assertThat(validator.isValid(yaml, context)).isFalse()
    }

    @Test
    fun `connector route URI with digit-leading suffix is invalid`() {
        val yaml = """
            - route:
                id: "route-main"
                from:
                  uri: "direct:iko:connector:foo"
                  steps:
                    - log: "main"
            - route:
                id: "route-bad"
                from:
                  uri: "direct:iko:connector:foo:1auth"
                  steps:
                    - log: "bad"
        """.trimIndent()
        assertThat(validator.isValid(yaml, context)).isFalse()
    }

    @Test
    fun `connector code with a transform route in the correct format is valid`() {
        val yaml = """
            - route:
                id: "route-main"
                from:
                  uri: "direct:iko:connector:foo"
                  steps:
                    - log: "main"
            - route:
                id: "route-transform"
                from:
                  uri: "direct:iko:endpoint:transform:foo.Personen"
                  steps:
                    - log: "transform"
        """.trimIndent()
        assertThat(validator.isValid(yaml, context)).isTrue()
    }

    @Test
    fun `bundled brp-wsgateway template fixture passes validation`() {
        val yaml = javaClass.getResource("/connectors/brp-wsgateway-template.yaml")!!.readText()
        assertThat(validator.isValid(yaml, context)).isTrue()
    }

    @Test
    fun `transform route with malformed shape is invalid`() {
        val yaml = """
            - route:
                id: "route-main"
                from:
                  uri: "direct:iko:connector:foo"
                  steps:
                    - log: "main"
            - route:
                id: "route-transform-bad"
                from:
                  uri: "direct:iko:endpoint:transform:foo"
                  steps:
                    - log: "bad-transform"
        """.trimIndent()
        assertThat(validator.isValid(yaml, context)).isFalse()
    }

    private fun yamlRoute(fromUri: String): String =
        """
        - route:
            id: "route-test"
            from:
              uri: "$fromUri"
              steps:
                - log: "test"
        """.trimIndent()
}