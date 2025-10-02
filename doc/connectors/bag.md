# BAG 

## Configuration

The configuration properties of openzaak are:
- **host**: Base URL of openzaak 
- **specificationUri**: The specification uri of openzaak (could be a file or a url)
- **secret**: The token to use for authentication

## Endpoints

BAG has the following endpoints:
- **bevraagAdressenMetNumId**
- **bevraagAdressen**

Other endpoints can be found by inspecting the specification.

## Connector Code

Copy the connector code down below and replace the `REFERENCE` with the refernce of the connector.`

```yaml
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.bevraagAdressenMetNumId"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.bevraagAdressenMetNumId"
          steps:
              - setHeader:
                    name: "nummeraanduidingIdentificatie"
                    simple: "${variable.id}"
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "nummeraanduidingIdentificatie|expand|inclusiefEindStatus"
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.bevraagAdressen"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.bevraagAdressen"
          steps:
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "zoekresultaatIdentificatie|postcode|huisnummer|huisnummertoevoeging|huisletter|exacteMatch|adresseerbareObjectIdentificatie|woonplaatsNaam|openbareRuimteNaam|pandIdentificatie|expand|page|pageSize|q|inclusiefEindStatus|openbareRuimteIdentificatie"
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
                    constant: "application/hal+json"
              - script:
                    groovy: |-
                        exchange.in.setHeader("X-Api-Key", "${exchange.getVariable('configProperties', Map).secret}")
              - log: "BODY: ${header.Accept}"
              - toD:
                    uri: "language:groovy:\"rest-openapi:${variable.configProperties.specificationUri}#${variable.operation}?host=${variable.configProperties.host}\""
              - unmarshal:
                    json: {}
```