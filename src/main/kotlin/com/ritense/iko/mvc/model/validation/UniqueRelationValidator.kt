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

import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component

@Component
class UniqueRelationValidator(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
) : ConstraintValidator<UniqueRelationCheck, UniqueRelation> {
    override fun isValid(
        form: UniqueRelation,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (form.propertyName.isBlank()) return false
        val aggregatedDataProfile = aggregatedDataProfileRepository.getReferenceById(form.aggregatedDataProfileId)
        val existing = aggregatedDataProfile.relations.find { it.id == form.id }
        val duplicateOnSameLevel = aggregatedDataProfile.relations
            .any { it.propertyName == form.propertyName && it.sourceId == form.sourceId }

        val isValid = duplicateOnSameLevel || existing?.id == form.id

        if (!isValid) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate("Name already exists/in use, please choose another.")
                .addPropertyNode("name") // point to 'name' field
                .addConstraintViolation()
        }
        return isValid
    }
}