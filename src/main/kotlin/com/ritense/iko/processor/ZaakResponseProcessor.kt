package com.ritense.iko.processor

import org.apache.camel.Exchange
import org.apache.camel.Processor

object ZaakResponseProcessor : Processor {
    override fun process(exchange: Exchange) {
        val body = exchange.getIn().body
        val processedBody = mapOf("zaak" to body)
        exchange.getIn().body = processedBody
    }
}