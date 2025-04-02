package com.ritense.iko.route.profiel.zaak

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestParamType.query

class ZakenProfielRoute : RouteBuilder() {
    override fun configure() {
        rest("profielen/zaken")
            .get()
            .param().name("bsn").type(query).description("Burgerservicenummer").endParam()
            .to("direct:zakenOpvragen")
    }
}