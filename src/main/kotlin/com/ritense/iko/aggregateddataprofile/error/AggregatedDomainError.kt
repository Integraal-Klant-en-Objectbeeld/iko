package com.ritense.iko.aggregateddataprofile.error

sealed interface Error {
    val message: String
}

sealed class DomainError(
    override val message: String,
) : RuntimeException(message),
    Error

sealed class AggregatedDataProfileDomainError(
    message: String,
) : DomainError(message)

class AggregatedDataProfileNotFound(
    name: String,
) : AggregatedDataProfileDomainError("ADP with name: $name, not found")

class AggregatedDataProfileQueryParametersError(
    vararg parameters: String,
) : AggregatedDataProfileDomainError("Query parameter(s) [${parameters.joinToString(", ")}] could not be parsed")