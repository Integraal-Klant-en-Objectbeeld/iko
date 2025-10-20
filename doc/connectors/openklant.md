# Openklant

## Configuration

The configuration properties of the objecten api are:
- **host**: Base URL 
- **specificationUri**: The specification uri 
- **token**: The token to use for authentication

## Endpoints

Openklant has the following endpoints:
- klantcontactenList
- actorenList

Other endpoints can be found by inspecting the specification.

## Connector Code

Copy the connector code down below and replace the `REFERENCE` with the refernce of the connector.`

```yaml
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.klantcontactenList"
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.klantcontactenList"
          steps:
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "expand|hadBetrokkene__url|hadBetrokkene__uuid|hadBetrokkene__wasPartij__partijIdentificator__codeObjecttype|hadBetrokkene__wasPartij__partijIdentificator__codeRegister|hadBetrokkene__wasPartij__partijIdentificator__codeSoortObjectId|hadBetrokkene__wasPartij__partijIdentificator__objectId|hadBetrokkene__wasPartij__url|hadBetrokkene__wasPartij__uuid|indicatieContactGelukt|inhoud|kanaal|nummer|onderwerp|onderwerpobject__onderwerpobjectidentificatorCodeObjecttype|onderwerpobject__onderwerpobjectidentificatorCodeRegister|onderwerpobject__onderwerpobjectidentificatorCodeSoortObjectId|onderwerpobject__onderwerpobjectidentificatorObjectId|onderwerpobject__url|onderwerpobject__uuid|page|pageSize|plaatsgevondenOp|vertrouwelijk|wasOnderwerpobject__onderwerpobjectidentificatorCodeObjecttype|wasOnderwerpobject__onderwerpobjectidentificatorCodeRegister|wasOnderwerpobject__onderwerpobjectidentificatorCodeSoortObjectId|wasOnderwerpobject__onderwerpobjectidentificatorObjectId|wasOnderwerpobject__url|wasOnderwerpobject__uuid"
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.actorenList"
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.actorenList"
          steps:
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "actoridentificatorCodeObjecttype|actoridentificatorCodeRegister|actoridentificatorCodeSoortObjectId|actoridentificatorObjectId|indicatieActief|naam|page|pageSize|soortActor"
- route:
      id: "direct:iko:connector:REFERENCE"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:connector:REFERENCE"
          steps:
              - setHeader:
                    name: "Accept"
                    constant: "application/json"
              - script:
                    groovy: |-
                        exchange.in.setHeader("Authorization", "Token ${exchange.getVariable('configProperties', Map).token}")
              - toD:
                    uri: "language:groovy:\"rest-openapi:${variable.configProperties.specificationUri}#${variable.operation}?host=${variable.configProperties.host}\""
              - unmarshal:
                    json: {}


```