package com.ritense.iko.source.bag.search

class BagSearchNummeraanduidingen : BagSearch() {
    companion object {
        val URI = "direct:bagSearchNummeraanduidingen"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(URI, "nummeraanduidingIdentificatie", "nummeraanduidingIdentificatie")

        searchRoute(
            URI, "zoekNummeraanduiding", listOf(
                "postcode",
                "huisnummer",
                "huisnummertoevoeging",
                "huisletter",
                "exacteMatch",
                "woonplaatsNaam",
                "openbareRuimteNaam",
                "openbareRuimteIdentificatie",
                "huidig",
                "geldigOp",
                "beschikbaarOp",
                "page",
                "pageSize",
                "expand",
                "pandIdentificatie"
            )
        )

    }
}