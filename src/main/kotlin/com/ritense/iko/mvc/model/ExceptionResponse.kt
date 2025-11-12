package com.ritense.iko.mvc.model

data class ExceptionResponse(
    val message: String? = null,
    val stacktrace: String? = null,
) {
    companion object {

        fun of(exception: Exception): ExceptionResponse {
            return ExceptionResponse(
                message = exception.message,
                stacktrace = exception.stackTraceToString()
            )
        }
    }
}