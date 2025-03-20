package com.ritense.iko.processor

import org.apache.camel.Exchange
import org.apache.camel.Processor

object OpenProductResponseProcessor : Processor {
    override fun process(exchange: Exchange) {
        val body = exchange.getIn().body
        val processedBody = mapOf("product" to body)
        exchange.getIn().body = processedBody
    }
}