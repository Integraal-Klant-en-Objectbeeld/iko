package com.ritense.iko.camel

class IkoRouteHelper {
    companion object {
        fun iko(uri: String) = "direct:iko:$uri"

        fun api(uri: String? = null) = iko(uri?.let { "api:$it" } ?: "api")

        fun connector(uri: String? = null) = iko(uri?.let { "connector:$it" } ?: "connector")

        fun endpoint(uri: String? = null) = iko(uri?.let { "endpoint:$it" } ?: "endpoint")

        fun transform(uri: String? = null) = endpoint(uri?.let { "transform:$it" } ?: "transform")
    }
}