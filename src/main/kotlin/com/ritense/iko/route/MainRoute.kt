package com.ritense.iko.route

import com.ritense.iko.aggregator.ResponseAggregator
import org.apache.camel.builder.RouteBuilder

class MainRoute : RouteBuilder() {
    override fun configure() {
        // The outputs of all calls are:
        // - in parallel
        // - aggregated into a map and returned as result.
        onException(Exception::class.java)
            .to("direct:errorHandle")

        from("rest:get:hello")
            .multicast(ResponseAggregator)
            .parallelProcessing()
            .to("direct:haalcentraal")
            .to("direct:objectsApi")
            //.to("direct:openZaak")
            .end()
            .marshal()
            .json()
    }
}