```
- route:
      id: "direct:iko:endpoint:transform:kvk.getResults"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:kvk.getResults"
          steps:
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "kvkNummer|rsin|vestigingsnummer|naam|postcode|huisnummer|straatnaam|plaats|type|pagina|resultatenPerPagina"
- route:
      id: "direct:iko:connector:kvk"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:connector:kvk"
          steps:
              - setHeader:
                    name: "Content-Type"
                    constant: "application/json"
              - setHeader:
                    name: "Accept"
                    constant: "application/json"
              - script:
                    groovy: |-
                        exchange.in.setHeader("apikey", "${exchange.getVariable('configProperties', java.util.Map).secret}")
              - log: "BODY: ${headers}"
              - toD:
                    uri: "language:groovy:\"rest-openapi:${variable.configProperties.specificationUri}#${variable.operation}?host=${variable.configProperties.host}&basePath=/test/api/v2\""
              - unmarshal:
                    json: {}

```