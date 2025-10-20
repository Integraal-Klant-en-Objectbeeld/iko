package com.ritense.iko.aggregateddataprofile.camel

import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange

object PairAggregator : AggregationStrategy {
    override fun aggregate(oldExchange: Exchange?, newExchange: Exchange): Exchange {
        if (oldExchange == null) {
            newExchange.getIn().body = mutableMapOf(newExchange.getVariable("relationPropertyName") to newExchange.getIn().body)
            return newExchange
        }

        val oldBody = oldExchange.getIn().body as MutableMap<*, *>
        val newBody = newExchange.getIn().body
        (oldExchange.getIn().body as MutableMap<Any?, Any?>).put(newExchange.getVariable("relationPropertyName"), newBody)
        return oldExchange
    }
}