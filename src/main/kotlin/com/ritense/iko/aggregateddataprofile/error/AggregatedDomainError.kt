package com.ritense.iko.aggregateddataprofile.error

import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.IKO_CORRELATION_ID_VARIABLE
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.CamelExecutionException
import org.apache.camel.Exchange
import org.apache.camel.model.OnExceptionDefinition
import org.apache.camel.model.ProcessorDefinition
import org.springframework.http.HttpStatus

fun OnExceptionDefinition.errorResponse(
    status: HttpStatus,
    exposeMessage: Boolean = true,
): ProcessorDefinition<*> = this
    .handled(true)
    .process(errorResponseProcessor(status, exposeMessage))
    .marshal().json()
    .stop()

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

class AggregatedDataProfileUnsupportedEndpointTransformResultTypeError(
    type: String,
) : AggregatedDataProfileDomainError("Endpoint Transform result is unsupported. Expected ObjectNode; got $type.")

fun errorResponseProcessor(
    status: HttpStatus,
    exposeMessage: Boolean = true,
): (Exchange) -> Unit = { exchange ->
    val logger = KotlinLogging.logger {}
    val exception: Exception? = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception::class.java)
    // Retrieve the correlation ID from variables
    val correlationId: String? = exchange.getVariable(IKO_CORRELATION_ID_VARIABLE, String::class.java)
    fallbackForDebugTrace(exchange, exception)

    val errorCorrelationId = correlationId ?: exchange.exchangeId
    val errorMessage = exception?.message?.takeIf { exposeMessage } ?: "Unexpected error"

    exchange.message.setHeader("X-Correlation-Id", errorCorrelationId)
    exchange.message.setHeader(Exchange.HTTP_RESPONSE_CODE, status.value())
    exchange.message.body = mapOf(
        "message" to errorMessage,
        "correlationId" to errorCorrelationId,
    )

    logger.error(exception) { "Exception with correlationId: $correlationId" }
}

private fun fallbackForDebugTrace(exchange: Exchange, throwable: Exception?) {
    val ikoTraceId: String? = exchange.getVariable(IKO_TRACE_ID_VARIABLE, String::class.java)

    if (ikoTraceId != null) {
        throw CamelExecutionException(throwable?.message ?: "Unexpected error", exchange)
    }
}