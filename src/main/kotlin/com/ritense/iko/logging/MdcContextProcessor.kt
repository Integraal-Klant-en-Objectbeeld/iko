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

package com.ritense.iko.logging

import com.ritense.iko.camel.IkoConstants.Variables.IKO_CORRELATION_ID_VARIABLE
import com.ritense.iko.camel.IkoConstants.Variables.IKO_TRACE_ID_VARIABLE
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.spi.Synchronization
import org.slf4j.MDC

internal class MdcContextProcessor : Processor {
    override fun process(exchange: Exchange) {
        exchange.getVariable(IKO_CORRELATION_ID_VARIABLE, String::class.java)
            ?.let { MDC.put(IKO_CORRELATION_ID_VARIABLE, it) }
        exchange.getVariable(IKO_TRACE_ID_VARIABLE, String::class.java)
            ?.let { MDC.put(IKO_TRACE_ID_VARIABLE, it) }

        exchange.exchangeExtension.addOnCompletion(
            object : Synchronization {
                override fun onComplete(exchange: Exchange) = clearMdc()

                override fun onFailure(exchange: Exchange) = clearMdc()

                private fun clearMdc() {
                    MDC.remove(IKO_CORRELATION_ID_VARIABLE)
                    MDC.remove(IKO_TRACE_ID_VARIABLE)
                }
            },
        )
    }
}