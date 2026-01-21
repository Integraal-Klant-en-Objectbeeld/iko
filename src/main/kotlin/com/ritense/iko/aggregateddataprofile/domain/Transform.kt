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

package com.ritense.iko.aggregateddataprofile.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import net.thisptr.jackson.jq.JsonQuery
import net.thisptr.jackson.jq.Versions
import net.thisptr.jackson.jq.exception.JsonQueryException

@Embeddable
class Transform(
    @Column(name = "transform")
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