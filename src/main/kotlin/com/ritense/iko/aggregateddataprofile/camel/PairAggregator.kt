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

package com.ritense.iko.aggregateddataprofile.camel

import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange

object PairAggregator : AggregationStrategy {
    override fun aggregate(
        oldExchange: Exchange?,
        newExchange: Exchange,
    ): Exchange {
        // Detect error
        val exception = newExchange.getProperty(Exchange.EXCEPTION_CAUGHT)
        if (exception != null) {
            throw exception as Exception
        }

        if (oldExchange == null) {
            return newExchange
        }

        oldExchange.getIn().body =
            mapOf(
                "left" to oldExchange.getIn().body,
                "right" to newExchange.getIn().body,
            )

        return oldExchange
    }
}