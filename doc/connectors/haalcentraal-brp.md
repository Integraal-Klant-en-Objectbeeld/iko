# Haalcentraal BRP

## Configuration

The configuration properties of openzaak are:
- **host**: Base URL
- **specificationUri**: The specification uri (could be a file or a url)
- **secret**: The token to use for authentication

## Endpoints

Openzaak has the following endpoints:
- Personen 

Other endpoints can be found by inspecting the specification.

## Connector Code

Copy the connector code down below and replace the `REFERENCE` with the refernce of the connector.`

```yaml
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.Personen"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.Personen"
          steps:
              - setBody:
                    jq: |
                        {
                           type: (if (header("type") != null) then header("type") else "RaadpleegMetBurgerservicenummer" end),
                           fields: (if (header("fields") != null) then header("fields") | split(",") else ["burgerservicenummer","naam","geboorte","nationaliteiten","verblijfplaats","partners"] end),
                           gemeenteVanInschrijving: header("gemeenteVanInschrijving"),
                           inclusiefOverledenPersonen: header("inclusiefOverledenPersonen"),
                           geboortedatum: header("geboortedatum"),
                           geslachtsnaam: header("geslachtsnaam"),
                           geslacht: header("geslacht"),
                           voorvoegsel: header("voorvoegsel"),
                           voornamen: header("voornamen"),
                           burgerservicenummer: (if header("burgerservicenummer") != null then header("burgerservicenummer") | split(",") else [header("idParam")] end),
                           huisletter: header("huisletter"),
                           huisnummer: header("huisnummer"),
                           huisnummertoevoeging: header("huisnummertoevoeging"),
                           postcode: header("postcode"),
                           geboortedatum: header("geboortedatum"),
                           geslachtsnaam: header("geslachtsnaam"),
                           straat: header("straat"),
                           nummeraanduidingIdentificatie: header("nummeraanduidingIdentificatie"),
                           adresseerbaarObjectIdentificatie: header("adresseerbaarObjectIdentificatie")
                        } | with_entries(select(.value!=null))
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "type|fields|gemeenteVanInschrijving|inclusiefOverledenPersonen|geboortedatum|geslachtsnaam|geslacht|voorvoegsel|voornamen|burgerservicenummer|huisletter|huisnummer|huisnummertoevoeging|postcode|geboortedatum|geslachtsnaam|straat|nummeraanduidingIdentificatie|adresseerbaarObjectIdentificatie"
- route:
      id: "direct:iko:connector:REFERENCE"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:connector:REFERENCE"
          steps:
              - setHeader:
                    name: "Content-Type"
                    constant: "application/json"
              - setHeader:
                    name: "Accept"
                    constant: "application/json; charset=utf-8"
              - script:
                    groovy: |-
                        exchange.in.setHeader("X-Api-Key", "${exchange.getVariable('configProperties', Map).secret}")
              - log: "BODY: ${header.Accept}"
              - toD:
                    uri: "language:groovy:\"rest-openapi:${variable.configProperties.specificationUri}#${variable.operation}?host=${variable.configProperties.host}\""
              - unmarshal:
                    json: {}
```

If you want to output the response body to the console log, add the following line to the second route of the connector at the same level of `- unmarshal:`:
```yaml
              - log: "BODY: ${body}"
```