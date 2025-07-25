package com.ritense.iko.aggregateddataprofile

import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange

object PairAggregator : AggregationStrategy {
    override fun aggregate(oldExchange: Exchange?, newExchange: Exchange): Exchange {
        if (oldExchange == null) {
            return newExchange
        }

        oldExchange.getIn().body = mapOf(
            "left" to oldExchange.getIn().body,
            "right" to newExchange.getIn().body,
        )

        return oldExchange
    }
}