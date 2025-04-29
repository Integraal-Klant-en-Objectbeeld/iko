package com.ritense.iko.source.openzaak

import org.apache.camel.Exchange

class OpenZaakTokenService {

    fun generateToken(exchange: Exchange, clientId: String, secret: String) {
        exchange.getIn().setHeader("Authorization", "Bearer ${TokenGeneratorService().generateToken(clientId, secret)}")
    }

}