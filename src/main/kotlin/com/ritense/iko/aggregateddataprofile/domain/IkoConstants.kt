package com.ritense.iko.aggregateddataprofile.domain

object IkoConstants {
    object Headers {
        const val CONTAINER_PARAM_HEADER = "containerParam"
        const val IKO_PROFILE_PARAM_HEADER = "iko_profile"
        const val IKO_ID_PARAM_HEADER = "iko_id"
        const val IKO_ENDPOINT_TRANSFORM_CONTEXT_HEADER = "iko_endpointTransformContext"
        const val IKO_TRACE_ID_HEADER = "iko_trace_id"
    }
    object Variables {
        const val ENDPOINT_TRANSFORM_CONTEXT_VARIABLE = "endpointTransformContext"
        const val ENDPOINT_TRANSFORM_RESULT_VARIABLE = "endpointTransformResult"
    }
}