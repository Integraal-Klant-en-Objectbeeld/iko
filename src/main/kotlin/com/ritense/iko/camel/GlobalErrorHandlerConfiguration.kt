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

import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileAccessDenied
import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileNotFound
import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileQueryParametersError
import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileUnsupportedEndpointTransformResultTypeError
import com.ritense.iko.camel.IkoRouteHelper.Companion.GLOBAL_ERROR_HANDLER_CONFIGURATION
import com.ritense.iko.connectors.error.ConnectorAccessDenied
import org.apache.camel.builder.RouteConfigurationBuilder
import org.apache.camel.http.base.HttpOperationFailedException
import org.springframework.http.HttpStatus

class GlobalErrorHandlerConfiguration : RouteConfigurationBuilder() {
    override fun configuration() {
        routeConfiguration(GLOBAL_ERROR_HANDLER_CONFIGURATION)
            .onException(AggregatedDataProfileAccessDenied::class.java)
            .errorResponse(status = HttpStatus.UNAUTHORIZED, exposeMessage = false)
            .onException(ConnectorAccessDenied::class.java)
            .errorResponse(status = HttpStatus.UNAUTHORIZED, exposeMessage = false)
            .onException(HttpOperationFailedException::class.java)
            .errorResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, exposeMessage = false)
            .onException(AggregatedDataProfileUnsupportedEndpointTransformResultTypeError::class.java)
            .errorResponse(status = HttpStatus.BAD_REQUEST, exposeMessage = true)
            .onException(AggregatedDataProfileNotFound::class.java)
            .errorResponse(status = HttpStatus.NOT_FOUND)
            .onException(AggregatedDataProfileQueryParametersError::class.java)
            .errorResponse(status = HttpStatus.BAD_REQUEST, exposeMessage = true)
            // Global exception handler
            .onException(Exception::class.java)
            .errorResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, exposeMessage = false)
    }
}