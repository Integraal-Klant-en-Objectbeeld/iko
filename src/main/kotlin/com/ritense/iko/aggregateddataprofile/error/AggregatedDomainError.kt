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

package com.ritense.iko.aggregateddataprofile.error

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

class AggregatedDataProfileAccessDenied(
    name: String,
) : AggregatedDataProfileDomainError("ADP with name: $name, access denied")

class AggregatedDataProfileNotFound(
    name: String,
) : AggregatedDataProfileDomainError("ADP with name: $name, not found")

class AggregatedDataProfileQueryParametersError(
    vararg parameters: String,
) : AggregatedDataProfileDomainError("Query parameter(s) [${parameters.joinToString(", ")}] could not be parsed")

class AggregatedDataProfileUnsupportedEndpointTransformResultTypeError(
    type: String,
) : AggregatedDataProfileDomainError("Endpoint Transform result is unsupported. Expected ObjectNode; got $type.")