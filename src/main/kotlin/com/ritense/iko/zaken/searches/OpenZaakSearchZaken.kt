package com.ritense.iko.zaken.searches

import org.springframework.stereotype.Component

@Component
class OpenZaakSearchZaken : OpenZaakSearch() {
    companion object {
        val URI = "direct:openZaakSearchZaken"
    }

    override fun configure() {
        idAndSearchRoute(URI)

        idRoute(
            URI, "zaak_read", "uuid"
        ) { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("EPSG:4326"))
        }

        searchRoute(
            URI, "zaak_list", listOf(
                "archiefactiedatum",
                "archiefactiedatum__gt",
                "archiefactiedatum__isnull",
                "archiefactiedatum__lt",
                "archiefnominatie",
                "archiefnominatie__in",
                "archiefstatus",
                "archiefstatus__in",
                "bronorganisatie",
                "bronorganisatie__in",
                "einddatum",
                "einddatumGepland",
                "einddatumGepland__gt",
                "einddatumGepland__lt",
                "einddatum__gt",
                "einddatum__isnull",
                "einddatum__lt",
                "expand",
                "identificatie",
                "maximaleVertrouwelijkheidaanduiding",
                "ordering",
                "page",
                "registratiedatum",
                "registratiedatum__gt",
                "registratiedatum__lt",
                "rol__betrokkene",
                "rol__betrokkeneIdentificatie__medewerker__identificatie",
                "rol__betrokkeneIdentificatie__natuurlijkPersoon__anpIdentificatie",
                "rol__betrokkeneIdentificatie__natuurlijkPersoon__inpA_nummer",
                "rol__betrokkeneIdentificatie__natuurlijkPersoon__inpBsn",
                "rol__betrokkeneIdentificatie__nietNatuurlijkPersoon__annIdentificatie",
                "rol__betrokkeneIdentificatie__nietNatuurlijkPersoon__innNnpId",
                "rol__betrokkeneIdentificatie__organisatorischeEenheid__identificatie",
                "rol__betrokkeneIdentificatie__vestiging__vestigingsNummer",
                "rol__betrokkeneType",
                "rol__omschrijvingGeneriek",
                "startdatum",
                "startdatum__gt",
                "startdatum__gte",
                "startdatum__lt",
                "startdatum__lte",
                "uiterlijkeEinddatumAfdoening",
                "uiterlijkeEinddatumAfdoening__gt",
                "uiterlijkeEinddatumAfdoening__lt",
                "zaaktype"
            )
        ) { routeDefinition ->
            routeDefinition
                .setHeader("Accept-Crs", constant("EPSG:4326"))
        }
    }

}