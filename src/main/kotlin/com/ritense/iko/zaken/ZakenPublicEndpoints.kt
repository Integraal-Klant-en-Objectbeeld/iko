package com.ritense.iko.zaken

import com.ritense.iko.personen.ValidationException
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class ZakenPublicEndpoints : RouteBuilder() {
    override fun configure() {

        onException(ValidationException::class.java)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("[]"))

        rest("/zaken/searchWithBsn")
            .get()
            .to("direct:public_zaken_bsn")

        from("direct:public_zaken_bsn")
            .to("direct:zakenSearch_bsn")
            .marshal().json()

        rest("/zaken")
            .get("{id}")
            .to("direct:public_zaken")

        from("direct:public_zaken")
            .setHeader("uuid", header("id"))
            .to("direct:zakenSearch")
            .marshal().json()

    }
}