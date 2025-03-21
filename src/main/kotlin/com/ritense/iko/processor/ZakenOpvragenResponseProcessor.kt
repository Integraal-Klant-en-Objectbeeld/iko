package com.ritense.iko.processor

import org.apache.camel.Exchange
import org.apache.camel.Processor

object ZakenOpvragenResponseProcessor : Processor {
    override fun process(exchange: Exchange) {
        val body = exchange.getIn().body
        val processedBody = mapOf("zaken" to body)
        exchange.getIn().body = processedBody
    }
}