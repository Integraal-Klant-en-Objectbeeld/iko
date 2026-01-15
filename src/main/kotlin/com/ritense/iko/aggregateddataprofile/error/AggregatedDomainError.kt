package com.ritense.iko.aggregateddataprofile.error

import org.apache.camel.Exchange
import org.springframework.http.HttpStatus

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

fun errorResponseProcessor(
    status: HttpStatus,
    errorLabel: String,
    exposeMessage: Boolean = true,
): (Exchange) -> Unit = { exchange ->
    val ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception::class.java)
    exchange.message.setHeader(Exchange.HTTP_RESPONSE_CODE, status.value())
    exchange.message.body = mapOf(
        "error" to errorLabel,
        "message" to if (exposeMessage) ex?.message else "Unexpected error",
    )
}