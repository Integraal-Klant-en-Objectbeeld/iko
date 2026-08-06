/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
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

import jakarta.annotation.PostConstruct
import org.apache.camel.CamelContext
import org.apache.camel.component.http.HttpClientConfigurer
import org.apache.camel.component.http.HttpComponent
import org.springframework.stereotype.Component

/**
 * Installs [OutgoingHttpTraceInterceptor] on the shared `http`/`https` components so debug runs can observe
 * the real outgoing request rest-openapi builds and sends internally (it never appears on the Camel
 * exchange). The underlying HttpClient is built once and cached, so the configurer is registered at startup
 * before any request is made. Any pre-existing configurer is preserved and run first. The interceptor is
 * global on `http`/`https` but inert unless the request carries the `iko_trace_span` header (set only during
 * a debug run), so normal traffic is unaffected.
 */
@Component
internal class HttpTraceConfiguration(
    private val camelContext: CamelContext,
    registry: OutgoingHttpTraceRegistry,
) {
    private val traceConfigurer = HttpClientConfigurer { builder ->
        builder.addRequestInterceptorLast(OutgoingHttpTraceInterceptor(registry))
    }

    @PostConstruct
    fun register() {
        listOf("http", "https").forEach { scheme ->
            val component = camelContext.getComponent(scheme, HttpComponent::class.java) ?: return@forEach
            val existing = component.httpClientConfigurer
            component.httpClientConfigurer = if (existing == null) {
                traceConfigurer
            } else {
                HttpClientConfigurer { builder ->
                    existing.configureHttpClient(builder)
                    traceConfigurer.configureHttpClient(builder)
                }
            }
        }
    }
}