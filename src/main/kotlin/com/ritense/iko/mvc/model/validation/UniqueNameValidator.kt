package com.ritense.iko.mvc.model.validation

import com.ritense.iko.profile.ProfileRepository
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.stereotype.Component

@Component
class UniqueNameValidator(
    private val profileRepository: ProfileRepository
) : ConstraintValidator<UniqueName, String> {

    override fun isValid(name: String?, context: ConstraintValidatorContext): Boolean {
        if (name.isNullOrBlank()) return true // let @NotBlank handle this

        val exists: Boolean = profileRepository.existsByName(name)
        if (!exists) {
            return true
        } else {
            // Customize the validation message
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("Name already exists please choose another.")
                .addConstraintViolation()
            return false
        }
    }
}