package com.ritense.iko.personen

import org.apache.camel.Exchange
import org.apache.camel.Predicate
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.spi.PredicateExceptionFactory

class PersonenValidations : RouteBuilder() {
    companion object {
        val VALID_BSN = "direct:validBsn"
        val VALID_POSTCODE = "direct:validPostcode"
        val VALID_HUISNUMMER = "direct:validHuisnummer"
    }

    override fun configure() {
        from(VALID_BSN)
            .errorHandler(noErrorHandler())
            .validate(header("bsn").regex("^[0-9]{9}$"))
            .predicateExceptionFactory(ValidationExceptionFactory("Invalid `bsn`"))

        from(VALID_POSTCODE)
            .errorHandler(noErrorHandler())
            .validate(header("postcode").regex("^[0-9]{4}[a-zA-Z]{2}$"))
            .predicateExceptionFactory(ValidationExceptionFactory("Invalid `postcode`"))

        from(VALID_HUISNUMMER)
            .errorHandler(noErrorHandler())
            .validate(header("huisnummer").regex("^[0-9]+$"))
            .predicateExceptionFactory(ValidationExceptionFactory("Invalid `huisnummer`"))
    }
}

class ValidationExceptionFactory(private val msg: String) : PredicateExceptionFactory {
    override fun newPredicateException(exchange: Exchange?, predicate: Predicate?, nodeId: String?): Exception {
        return ValidationException("Validation Exception: `${msg}` with predicate `${predicate}`")
    }
}

class ValidationException(msg: String) : Exception(msg) {}
