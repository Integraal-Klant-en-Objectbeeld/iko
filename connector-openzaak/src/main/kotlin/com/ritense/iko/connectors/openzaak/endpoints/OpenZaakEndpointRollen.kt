package com.ritense.iko.connectors.openzaak.endpoints


class OpenZaakEndpointRollen : OpenZaakEndpoint() {
    companion object {
        val URI = "direct:openZaakEndpointRollen"
    }

    override fun configure() {
        idAndEndpointRoute(URI)

        idRoute(
            URI, "rol_read", "uuid"
        )

        endpointRoute(
            URI, "rol_list", listOf(
                "betrokkene",
                "betrokkeneIdentificatie__medewerker__identificatie",
                "betrokkeneIdentificatie__natuurlijkPersoon__anpIdentificatie",
                "betrokkeneIdentificatie__natuurlijkPersoon__inpA_nummer",
                "betrokkeneIdentificatie__natuurlijkPersoon__inpBsn",
                "betrokkeneIdentificatie__nietNatuurlijkPersoon__annIdentificatie",
                "betrokkeneIdentificatie__nietNatuurlijkPersoon__innNnpId",
                "betrokkeneIdentificatie__organisatorischeEenheid__identificatie",
                "betrokkeneIdentificatie__vestiging__vestigingsNummer",
                "betrokkeneType",
                "omschrijving",
                "omschrijvingGeneriek",
                "page",
                "roltype",
                "zaak",
            )
        )
    }

}