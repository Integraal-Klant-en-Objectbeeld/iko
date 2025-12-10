package com.ritense.iko.mvc.model.validation

import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component

@Component
class UniqueCollectionCheckValidator(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
) : ConstraintValidator<UniqueCollectionCheck, UniqueAggregatedDataProfile> {
    override fun isValid(
        form: UniqueAggregatedDataProfile,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (form.name.isBlank()) return false

        val existing = aggregatedDataProfileRepository.findByName(form.name)
        val isValid = existing == null || existing.id == form.id

        if (!isValid) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate("Name already exists, please choose another.")
                .addPropertyNode("name") // point to 'name' field
                .addConstraintViolation()
        }

        return isValid
    }
}