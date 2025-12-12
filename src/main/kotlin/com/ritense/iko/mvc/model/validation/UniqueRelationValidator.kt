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
        val existing = aggregatedDataProfile.relations.find { it.id == form.id}
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