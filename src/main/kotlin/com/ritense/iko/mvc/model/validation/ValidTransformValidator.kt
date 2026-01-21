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

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import net.thisptr.jackson.jq.JsonQuery
import net.thisptr.jackson.jq.Versions
import net.thisptr.jackson.jq.exception.JsonQueryException

class ValidTransformValidator : ConstraintValidator<ValidTransform, String> {
    override fun isValid(
        expression: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (expression.isNullOrBlank()) return true // let @NotBlank handle this

        return try {
            JsonQuery.compile(expression, Versions.JQ_1_6)
            true
        } catch (e: JsonQueryException) {
            // Customize the validation message
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate("Invalid jq expression: ${e.message}")
                .addConstraintViolation()
            false
        }
    }
}