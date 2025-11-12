```
- route:
      id: "direct:iko:endpoint:transform:brk.GetKadastraalOnroerendeZaken"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:brk.GetKadastraalOnroerendeZaken"
          steps:
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "Accept-Crs|expand|fields|kadastraleAanduiding|persoon__identificatie|postcode|huisnummer|huisletter|huisnummertoevoeging|nummeraanduidingIdentificatie|kadastraleAanduidingMetGemeentecode|inclusiefKadastraalOnroerendeZakenUitSplitsing"
              - setHeader:
                    name: "Accept-Crs"
                    constant: "epsg:28992"
- route:
      id: "direct:iko:connector:brk"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:connector:brk"
          steps:
              - setHeader:
                    name: "Content-Type"
                    constant: "application/json"
              - setHeader:
                    name: "Accept"
                    constant: "application/hal+json"
              - script:
                    groovy: |-
                        exchange.in.setHeader("X-Api-Key", "${exchange.getVariable('configProperties', Map).secret}")
              - log: "BODY: ${headers}"
              - toD:
                    uri: "language:groovy:\"rest-openapi:${variable.configProperties.specificationUri}#${variable.operation}?host=${variable.configProperties.host}&basePath=/esd-eto-apikey/bevragen/v2\""
              - unmarshal:
                    json: {}
```