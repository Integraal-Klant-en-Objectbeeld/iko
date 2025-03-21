package com.ritense.iko.route.persoongegeven

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestParamType.query

class PersoonsgegevensProfielRoute : RouteBuilder() {
    override fun configure() {
        rest("profiel/persoongegeven")
            .get()
            .param().name("bsn").type(query).description("Burgerservicenummer").endParam()
            .to("direct:raadpleegMetBurgerservicenummer")
    }
}