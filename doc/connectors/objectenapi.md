# Objecten API

## Configuration

The configuration properties of the objecten api are:
- **host**: Base URL of the objecten api
- **specificationUri**: The specification uri of the objecten api (could be a file or a url)
- **token**: The token to use for authentication

## Endpoints

The objecten api has the following endpoints:
- **object_list**: Get a list of objects
- **object_read**: Get a single object

Other endpoints can be found by inspecting the specification.

## Connector Code

Copy the connector code down below and replace the `REFERENCE` with the refernce of the connector.`

```yaml
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.object_list"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.object_list"
          steps:
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "data_attrs|data_attr|date|fields|ordering|page|pageSize|registrationDate|type"
              - setHeader:
                    name: "Accept-Crs"
                    constant: "EPSG:28992"
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.object_read"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.object_read"
          steps:
              - setHeader:
                    name: "uuid"
                    variable: "id"
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "uuid|fields"
              - setHeader:
                    name: "Accept-Crs"
                    constant: "EPSG:28992"
- route:
      id: "direct:iko:connector:REFERENCE"
      errorHandler:
        noErrorHandler: {}
      from:
          uri: "direct:iko:connector:REFERENCE"
          steps:
              - removeHeaders:
                    pattern: "*"
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