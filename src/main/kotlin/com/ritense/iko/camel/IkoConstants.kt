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