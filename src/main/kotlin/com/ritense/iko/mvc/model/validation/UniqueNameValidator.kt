package com.ritense.iko.mvc.model.validation

import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.mvc.model.AggregatedDataProfileForm
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component

@Component
class UniqueNameValidator(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
) : ConstraintValidator<UniqueName, AggregatedDataProfileForm> {
    override fun isValid(
        form: AggregatedDataProfileForm,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (form.name.isBlank()) return true // Let @NotBlank handle this

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