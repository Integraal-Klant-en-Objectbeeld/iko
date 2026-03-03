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

package com.ritense.iko.aggregateddataprofile.processor

import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileNotFound
import com.ritense.iko.aggregateddataprofile.error.AggregatedDataProfileSchemaNotAvailable
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.camel.IkoConstants.Headers
import com.ritense.iko.camel.IkoConstants.Variables
import org.apache.camel.Exchange

class AggregatedDataProfileSchemaProcessor(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
) {

    fun resolveProfile(exchange: Exchange) {
        val profileName = exchange.`in`.getHeader(
            Headers.ADP_PROFILE_NAME_PARAM_HEADER,
            String::class.java,
        )
        val adp = aggregatedDataProfileRepository.findByNameAndIsActiveTrue(profileName)
            ?: throw AggregatedDataProfileNotFound(profileName)
        exchange.setVariable(Variables.PROFILE_NAME, adp.name)
        exchange.setVariable(Variables.AUTHORITIES, adp.roles.asList())
        exchange.setVariable(ADP_SCHEMA, adp.jsonschema)
    }

    fun resolveSchema(exchange: Exchange) {
        val schema = exchange.getVariable(ADP_SCHEMA, String::class.java)
            ?: throw AggregatedDataProfileSchemaNotAvailable(
                exchange.getVariable(Variables.PROFILE_NAME, String::class.java) ?: "unknown",
            )
        exchange.`in`.body = schema
    }

    companion object {
        private const val ADP_SCHEMA = "adpSchema"
    }
}