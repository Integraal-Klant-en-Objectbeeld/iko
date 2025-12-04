package com.ritense.iko.mvc.model

import org.apache.camel.CamelExecutionException

data class ExceptionResponse(
    val message: String? = null,
    val stacktrace: String? = null,
) {
    companion object {
        fun of(exception: CamelExecutionException): ExceptionResponse = ExceptionResponse(
            message = exception.message,
            stacktrace = exception.stackTraceToString(),
        )
    }
}