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

import com.ritense.iko.connectors.service.ConnectorService
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidConnectorCodeValidator(
    private val connectorService: ConnectorService,
) : ConstraintValidator<ValidConnectorCode, String> {

    override fun isValid(
        connectorCode: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (connectorCode.isNullOrBlank()) return true // let @NotBlank handle this

        return try {
            // Use a placeholder tag for validation - the tag doesn't affect YAML parsing
            connectorService.validateConnectorCode(connectorCode, "validation-check")
            true
        } catch (e: Exception) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate("Invalid connector code: ${e.message}")
                .addConstraintViolation()
            false
        }
    }
}