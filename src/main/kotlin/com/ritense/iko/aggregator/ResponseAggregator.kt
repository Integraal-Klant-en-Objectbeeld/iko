package com.ritense.iko.aggregator

import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange

object ResponseAggregator : AggregationStrategy {
    override fun aggregate(oldExchange: Exchange?, newExchange: Exchange): Exchange {
        if (oldExchange == null) {
            return newExchange
        }
        val oldBody = oldExchange.getIn().body as Map<*, *>
        val newBody = newExchange.getIn().body as Map<*, *>
        oldExchange.getIn().body = oldBody + newBody
        return oldExchange
    }
}