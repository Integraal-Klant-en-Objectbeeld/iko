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

object IkoConstants {
    object Headers {
        const val ADP_PROFILE_NAME_PARAM_HEADER = "adp_profileName"
        const val ADP_CONTAINER_PARAM_HEADER = "containerParam"
        const val ADP_ID_PARAM_HEADER = "id"
        const val ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER = "adp_endpointTransformContext"
    }
    object Variables {
        const val IKO_CORRELATION_ID_VARIABLE = "correlationId"
        const val IKO_TRACE_ID_VARIABLE = "iko_trace_id"
        const val ENDPOINT_TRANSFORM_CONTEXT_VARIABLE = "endpointTransformContext"
        const val ENDPOINT_TRANSFORM_RESULT_VARIABLE = "endpointTransformResult"
    }
    object Validation {
        const val ROLES_PATTERN = "^[A-Za-z0-9_-]+(,[A-Za-z0-9_-]+)*$"
    }
}