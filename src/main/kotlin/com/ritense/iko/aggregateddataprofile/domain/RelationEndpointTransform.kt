package com.ritense.iko.aggregateddataprofile.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import net.thisptr.jackson.jq.JsonQuery
import net.thisptr.jackson.jq.Versions
import net.thisptr.jackson.jq.exception.JsonQueryException

@Embeddable
class RelationEndpointTransform(
    @Column(name = "source_to_endpoint_mapping")
    val expression: String,
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