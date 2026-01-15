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