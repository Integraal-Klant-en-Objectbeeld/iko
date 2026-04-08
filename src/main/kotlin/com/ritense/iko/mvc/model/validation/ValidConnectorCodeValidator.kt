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

import com.ritense.iko.camel.IkoConstants.Validation.CONNECTOR_CODE_CONNECTOR_ROUTE_PATTERN
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.support.PluginHelper
import org.apache.camel.support.ResourceHelper

class ValidConnectorCodeValidator(
    private val camelContext: CamelContext,
) : ConstraintValidator<ValidConnectorCode, String> {

    override fun isValid(
        connectorCode: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (connectorCode.isNullOrBlank()) return true // let @NotBlank handle this

        return try {
            validate(connectorCode)
            true
        } catch (e: Exception) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate("Invalid connector code: ${e.message}")
                .addConstraintViolation()
            false
        }
    }

    private fun validate(connectorCode: String) {
        val resource = ResourceHelper.fromString("connector-validation.yaml", connectorCode)
        val loader = PluginHelper.getRoutesLoader(camelContext)
        val builders = loader.findRoutesBuilders(listOf(resource))

        val allUris = builders.flatMap { builder ->
            val routeBuilder = builder as RouteBuilder
            routeBuilder.setCamelContext(camelContext)
            routeBuilder.configure()
            routeBuilder.routeCollection.routes.map { it.input.uri }
        }

        require(allUris.any { CONNECTOR_URI_REGEX.matches(it) }) {
            "Connector code must contain at least one 'direct:iko:connector:<tag>' route"
        }

        allUris.filter { TRANSFORM_PREFIX_REGEX.containsMatchIn(it) }.forEach { uri ->
            require(TRANSFORM_URI_REGEX.matches(uri)) {
                "Transform route '$uri' must use format 'direct:iko:endpoint:transform:<tag>.<operation>'"
            }
        }
    }

    companion object {
        private val CONNECTOR_URI_REGEX = Regex(CONNECTOR_CODE_CONNECTOR_ROUTE_PATTERN)
        private val TRANSFORM_URI_REGEX = Regex("""^direct:iko:endpoint:transform:[^:.]+\.[^:.]+$""")
        private val TRANSFORM_PREFIX_REGEX = Regex("""^direct:iko:endpoint:transform:""")
    }
}