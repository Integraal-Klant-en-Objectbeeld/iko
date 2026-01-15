package com.ritense.iko.aggregateddataprofile.camel

import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange

object MapAggregator : AggregationStrategy {
    override fun aggregate(
        oldExchange: Exchange?,
        newExchange: Exchange,
    ): Exchange {
        if (oldExchange == null) {
            newExchange.getIn().body = mutableMapOf(newExchange.getVariable("relationPropertyName") to newExchange.getIn().body)
            return newExchange
        }

        val oldBody = oldExchange.getIn().getBody(MutableMap::class.java) as MutableMap<String, Any>
        val newBody = newExchange.getIn().body
        oldBody[newExchange.getVariable("relationPropertyName") as String] = newBody
        return oldExchange
    }
}