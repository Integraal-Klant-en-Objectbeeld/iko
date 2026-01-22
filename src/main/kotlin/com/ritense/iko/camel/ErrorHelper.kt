/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.camel

import com.ritense.iko.camel.IkoConstants.Variables.IKO_CORRELATION_ID_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.CamelExecutionException
import org.apache.camel.Exchange
import org.apache.camel.model.OnExceptionDefinition
import org.apache.camel.model.ProcessorDefinition
import org.springframework.http.HttpStatus

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

fun OnExceptionDefinition.errorResponse(
    status: HttpStatus,
    exposeMessage: Boolean = true,
): ProcessorDefinition<*> = this
    .handled(true)
    .process(errorResponseProcessor(status, exposeMessage))
    .marshal().json()
    .stop()