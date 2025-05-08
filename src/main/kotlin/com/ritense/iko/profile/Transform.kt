package com.ritense.iko.profile

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import net.thisptr.jackson.jq.JsonQuery
import net.thisptr.jackson.jq.Versions
import net.thisptr.jackson.jq.exception.JsonQueryException

@Embeddable
class Transform(
    @Column(name = "transform")
    val expression: String
) {
    init {
        validate(expression)
    }

    companion object {
        fun validate(expression: String) {
            try {
                JsonQuery.compile(expression, Versions.JQ_1_6)
            } catch (e: JsonQueryException) {
                throw IllegalArgumentException("Invalid expression: $expression", e)
            }
        }
    }
}