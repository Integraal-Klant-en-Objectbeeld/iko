package com.ritense.iko.aggregateddataprofile.camel

import org.apache.camel.AggregationStrategy
import org.apache.camel.Exchange

object MapAggregator : AggregationStrategy {
    override fun aggregate(
        oldExchange: Exchange?,
        newExchange: Exchange,
    ): Exchange {
        val childRelationKey = newExchange.getVariable("relationPropertyName")
        if (oldExchange == null) {
            newExchange.getIn().body = mutableMapOf(childRelationKey to newExchange.getIn().body)
            return newExchange
        }

        val oldBody = oldExchange.getIn().getBody(MutableMap::class.java)
        val newBody = newExchange.getIn().body
        oldBody.toMutableMap().put(childRelationKey, newBody)
        return oldExchange
    }
}