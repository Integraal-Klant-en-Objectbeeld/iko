package com.ritense.iko.mvc.model.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import net.thisptr.jackson.jq.JsonQuery
import net.thisptr.jackson.jq.Versions
import net.thisptr.jackson.jq.exception.JsonQueryException

class ValidTransformValidator : ConstraintValidator<ValidTransform, String> {

    override fun isValid(expression: String?, context: ConstraintValidatorContext): Boolean {
        if (expression.isNullOrBlank()) return true // let @NotBlank handle this

        return try {
            JsonQuery.compile(expression, Versions.JQ_1_6)
            true
        } catch (e: JsonQueryException) {
            // Customize the validation message
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("Invalid jq expression: ${e.message}")
                   .addConstraintViolation()
            false
        }
    }
}