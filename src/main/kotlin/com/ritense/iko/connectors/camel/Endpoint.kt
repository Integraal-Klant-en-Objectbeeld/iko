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

package com.ritense.iko.connectors.camel

import org.apache.camel.builder.RouteBuilder

class Endpoint : RouteBuilder() {
    override fun configure() {
        rest("/endpoints")
            .get("/{iko_connector}/{iko_config}/{iko_operation}")
            .to(Iko.iko("rest:endpoint"))
            .get("/{iko_connector}/{iko_config}/{iko_operation}/{id}")
            .to(Iko.iko("rest:endpoint.id"))

        from(Iko.iko("rest:endpoint"))
            .routeId("rest-endpoint")
            .errorHandler(noErrorHandler())
            .setVariable("connector", header("iko_connector"))
            .setVariable("config", header("iko_config"))
            .setVariable("operation", header("iko_operation"))
            .removeHeaders("iko_*")
            .to(Iko.endpoint("validate"))
            .to(Iko.endpoint("auth"))
            .to(Iko.iko("config"))
            .to(Iko.transform())
            .to(Iko.connector())
            .marshal()
            .json()

        from(Iko.iko("rest:endpoint.id"))
            .routeId("rest-endpoint-id")
            .errorHandler(noErrorHandler())
            .setVariable("connector", header("iko_connector"))
            .setVariable("config", header("iko_config"))
            .setVariable("operation", header("iko_operation"))
            .setVariable("id", header("id"))
            .removeHeaders("iko_*")
            .to(Iko.endpoint("validate"))
            .to(Iko.endpoint("auth"))
            .to(Iko.iko("config"))
            .to(Iko.transform())
            .toD(Iko.connector())
            .marshal()
            .json()
    }
}