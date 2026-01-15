package com.ritense.iko.aggregateddataprofile.domain

object IkoConstants {
    object Headers {
        const val ADP_PROFILE_NAME_PARAM_HEADER = "adp_profileName"
        const val ADP_CONTAINER_PARAM_HEADER = "containerParam"
        const val ADP_ID_PARAM_HEADER = "id"
        const val ADP_ENDPOINT_TRANSFORM_CONTEXT_HEADER = "adp_endpointTransformContext"
        const val IKO_TRACE_ID_HEADER = "iko_trace_id"
    }
    object Variables {
        const val ENDPOINT_TRANSFORM_CONTEXT_VARIABLE = "endpointTransformContext"
        const val ENDPOINT_TRANSFORM_RESULT_VARIABLE = "endpointTransformResult"
    }
}