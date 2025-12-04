package com.ritense.iko.mvc.model.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidTransformValidator::class])
annotation class ValidTransform(
    val message: String = "Invalid jq expression",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)