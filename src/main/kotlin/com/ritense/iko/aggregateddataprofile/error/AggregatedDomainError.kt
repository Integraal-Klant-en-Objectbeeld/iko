package com.ritense.iko.aggregateddataprofile.error

sealed interface Error {
    val message: String
}

sealed class DomainError(
    override val message: String,
) : Error, RuntimeException(message)

sealed class AggregatedDataProfileDomainError(
    message: String,
) : DomainError(message)

class AggregatedDataProfileNotFound(
    name: String,
) : AggregatedDataProfileDomainError("ADP with name: $name, not found")