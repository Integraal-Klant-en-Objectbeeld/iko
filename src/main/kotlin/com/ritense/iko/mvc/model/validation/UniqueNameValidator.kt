package com.ritense.iko.mvc.model.validation

import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component

@Component
class UniqueNameValidator(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
) : ConstraintValidator<UniqueName, UniqueNameForm> {
    override fun isValid(
        form: UniqueNameForm,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (form.name.isBlank()) return false

        val found = aggregatedDataProfileRepository.existsByNameLikeIgnoreCase(form.name)

        if (found) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate("Name already exists, please choose another.")
                .addPropertyNode("name")
                .addConstraintViolation()
            return false
        }
        return true
    }
}