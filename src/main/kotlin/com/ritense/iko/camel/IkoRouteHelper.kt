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

class IkoRouteHelper {
    companion object {
        const val GLOBAL_ERROR_HANDLER_CONFIGURATION = "global-error-handler-configuration"

        fun iko(uri: String) = "direct:iko:$uri"

        fun api(uri: String? = null) = iko(uri?.let { "api:$it" } ?: "api")

        fun connector(uri: String? = null) = iko(uri?.let { "connector:$it" } ?: "connector")

        fun endpoint(uri: String? = null) = iko(uri?.let { "endpoint:$it" } ?: "endpoint")

        fun transform(uri: String? = null) = endpoint(uri?.let { "transform:$it" } ?: "transform")
    }
}