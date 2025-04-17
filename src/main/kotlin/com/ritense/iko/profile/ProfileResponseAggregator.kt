package com.ritense.iko.profile

import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange

object ProfileResponseAggregator : AggregationStrategy {
    override fun aggregate(oldExchange: Exchange?, newExchange: Exchange): Exchange {
        if (oldExchange == null) {
            return newExchange
        }

        oldExchange.getIn().body = mapOf(
            "primary" to oldExchange.getIn().body,
            "secondary" to newExchange.getIn().body,
        )

        return oldExchange
    }
}