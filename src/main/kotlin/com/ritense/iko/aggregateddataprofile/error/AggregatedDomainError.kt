package com.ritense.iko.aggregateddataprofile.error

import io.github.oshai.kotlinlogging.KotlinLogging
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.IKO_CORRELATION_ID_VARIABLE
import com.ritense.iko.aggregateddataprofile.domain.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import org.apache.camel.CamelExecutionException
import org.apache.camel.Exchange
import org.apache.camel.model.OnExceptionDefinition
import org.apache.camel.model.ProcessorDefinition
import org.springframework.http.HttpStatus

fun OnExceptionDefinition.errorResponse(
    status: HttpStatus,
    exposeMessage: Boolean = true,
): ProcessorDefinition<*> = this.handled(true)
    .process(errorResponseProcessor(status, exposeMessage))
    .marshal().json()

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
    val ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception::class.java)
    // Retrieve the correlation ID from variables
    val correlationId: String? = exchange.getVariable(IKO_CORRELATION_ID_VARIABLE, String::class.java)
    val ikoTraceId: String? = exchange.getVariable(IKO_TRACE_ID_VARIABLE, String::class.java)

    if (ikoTraceId != null) {
        throw CamelExecutionException(ex.message ?: "Unexpected error", exchange)
    }

    exchange.message.setHeader("X-Correlation-Id", correlationId)
    exchange.message.setHeader(Exchange.HTTP_RESPONSE_CODE, status.value())
    exchange.message.body =
        jacksonObjectMapper()
            .createObjectNode()
            .apply {
                put(IKO_CORRELATION_ID_VARIABLE, correlationId ?: exchange.exchangeId)
                put("message", ex?.message?.takeIf { exposeMessage } ?: "Unexpected error")
            }
    logger.error(ex) { "Exception with correlationId: $correlationId" }
}