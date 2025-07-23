package com.ritense.iko.mvc.model.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [UniqueNameValidator::class])
annotation class UniqueName(
    val message: String = "Name already exists please choose another.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)