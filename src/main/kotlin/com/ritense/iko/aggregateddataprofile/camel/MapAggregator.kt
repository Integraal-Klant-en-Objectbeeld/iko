package com.ritense.iko.aggregateddataprofile.camel

import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange

object MapAggregator : AggregationStrategy {
    override fun aggregate(
        oldExchange: Exchange?,
        newExchange: Exchange,
    ): Exchange {
        // Detect error
        val exception = newExchange.getProperty(Exchange.EXCEPTION_CAUGHT)
        if (exception != null) {
            throw exception as Exception
        }

        val key = newExchange.getVariable("relationPropertyName", String::class.java)
        val value = newExchange.getIn().body

        return if (oldExchange == null) {
            newExchange.getIn().body = mutableMapOf(key to value)
            newExchange
        } else {
            val map = oldExchange.getIn().getBody(MutableMap::class.java) as MutableMap<String, Any>
            map[key] = value
            oldExchange
        }
    }
}