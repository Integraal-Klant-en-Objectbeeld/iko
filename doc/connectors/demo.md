```yaml
- route:
      id: "getMockData"
      from:
          uri: "direct:iko:connector:demo"
          steps:
              - setBody:
                    constant: '{
"content" : [ {
  "basisgegevens": {
    "naam": "Sofia Rahman",
    "geslacht": "Vrouw",
    "geboortedatum": "1985-08-08",
    "bsn": "987654321",
    "adres": "Acacialaan 14, 3523GH Utrecht",
    "telefoon": "06-34567890",
    "e_mail": "sofia.rahman@example.com",
    "nationaliteit": "Nederlands",
    "burgerlijke_staat": "Alleenstaand"
  },
  "werkprofiel": {
    "huidig_dienstverband": {
      "werkgever": "Kinderdagverblijf zonnestraal",
      "functie": "Pedagogisch medewerker",
      "uren_per_week": 32
    },
    "arbeidsverleden": "8 jaar ervaring in kinderopvang en onderwijsassistentie",
    "opleidingsniveau": "MBO pedagogisch werk",
    "re_integratietraject": "Geen, wel recent scholingstraject kinderpsychologie afgerond"
  },
  "inkomensprofiel": {
    "primair_inkomen": "€2300 bruto per maand",
    "aanvullende_inkomsten": "Geen",
    "uitkering": "Geen, wel recht op zorg- en huurtoeslag",
    "schulden": "Geen noemenswaardige schulden",
    "vermogen": "Spaarsaldo €1200"
  },
  "gezinsprofiel": {
    "gezin": {
      "kinderen": [
        {
          "naam": "Amina",
          "leeftijd": 7,
           "relatie": "Kind" 
        },
{
          "naam": "Jan",
          "leeftijd": 12,
           "relatie": "Kind" 
        }
      ]
    },
    "mantelzorg": "Ondersteunt haar tante (65) met wekelijkse boodschappen",
    "huishoudsamenstelling": "Eenoudergezin",
    "zorgbehoefte": "Dochter volgt logopediebegeleiding"
  },
  "producten_en_voorzieningen": {
    "overheidsdocumenten": {
      "rijbewijs_b": "geldig tot 2031",
      "paspoort": "geldig tot 2033"
    },
    "toeslagen": [
      {"type": "Huurtoeslag"},
      {"type": "Zorgtoeslag"},
      {"type": "Kindgebonden budget"}
    ],
    "gemeentelijke_regelingen": [
      "bijzondere bijstand voor schoolspullen"
    ]
  },
  "lopende_zaken": [
    {
      "type": "Bijzondere bijstand",
      "beschrijving": "Aanvraag voor logopediekosten in behandeling"
    },
    {
      "type": "Jeugdhulp",
      "beschrijving": "Dossier voor logopedie dochter"
    },
    {
      "type": "Wijkteam",
      "beschrijving": "Contact voor ondersteuning kinderopvangtoeslag"
    }
  ],
  "contactmomenten": [
    {
      "kanaal": "Telefonisch",
      "beschrijving": "Gesprek met jeugdhulpcoördinator 2 weken geleden"
    },
    {
      "kanaal": "E-mail",
      "beschrijving": "Bevestiging toekenning huurtoeslag vorige maand"
    },
    {
      "kanaal": "Fysiek",
      "beschrijving": "Huisbezoek door wijkteam 3 maanden geleden"
    },
    {
      "kanaal": "Digitaal portaal",
      "beschrijving": "Laatste login 1 week geleden voor upload documenten"
    }
  ],
  "notities": [
    {
      "categorie": "casemanager",
      "inhoud": "sofia is proactief in het zoeken van hulp en houdt afspraken goed na."
    },
    {
      "categorie": "opmerking",
      "inhoud": "voorkeur voor digitale communicatie; reageert snel via e-mail."
    },
    {
      "categorie": "vervolgactie",
      "inhoud": "herbeoordeling bijzondere bijstand gepland op 20 oktober."
    }
  ]
}]}'

              - unmarshal:
                    json: {}
              - setHeader:
                    name: "Content-Type"
                    constant: "application/json"
```