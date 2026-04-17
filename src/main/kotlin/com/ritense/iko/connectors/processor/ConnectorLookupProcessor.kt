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

package com.ritense.iko.connectors.processor

import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_ID_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_TAG_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.CONNECTOR_VERSION_VARIABLE
import com.ritense.iko.connectors.repository.ConnectorRepository
import org.apache.camel.Exchange
import org.apache.camel.Processor

class ConnectorLookupProcessor(
    private val connectorRepository: ConnectorRepository,
) : Processor {
    override fun process(exchange: Exchange) {
        val connectorTag = exchange.getVariable(CONNECTOR_TAG_VARIABLE, String::class.java)
        val connector = requireNotNull(connectorRepository.findByTagAndIsActiveTrue(connectorTag)) {
            "Connector with tag [$connectorTag] not found"
        }

        with(connector) {
            exchange.setVariable(CONNECTOR_ID_VARIABLE, id)
            exchange.setVariable(CONNECTOR_VERSION_VARIABLE, version.toString())
        }
    }
}