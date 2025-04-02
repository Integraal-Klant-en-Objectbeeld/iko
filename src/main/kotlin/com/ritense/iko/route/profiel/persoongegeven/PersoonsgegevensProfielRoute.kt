package com.ritense.iko.route.profiel.persoongegeven

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestParamType.query

class PersoonsgegevensProfielRoute : RouteBuilder() {
    override fun configure() {
        rest("profielen/persoonsgegevens")
            .description("Persoonsgegevens raadplegen")
            .get()
                .id("getPersoonsgegevens")
                .description("Raadpleeg persoonsgegevens op basis van BSN")
                .param()
                    .name("bsn")
                    .type(query)
                    .description("Burgerservicenummer")
                    .dataType("string")
                    .required(true)
                .endParam()
                .responseMessage()
                    .code(200)
                    .message("Persoonsgegevens opgehaald")
                .endResponseMessage()
            .to("direct:raadpleegMetBurgerservicenummer")
    }
}