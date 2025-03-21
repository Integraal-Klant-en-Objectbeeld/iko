package com.ritense.iko.route.profiel

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestParamType.query

class ZakenProfielRoute : RouteBuilder() {
    override fun configure() {
        rest("profiel/zaken")
            .get()
            .param().name("bsn").type(query).description("Burger Service Nummer").endParam()
            .to("direct:zakenOpvragen")
    }
}