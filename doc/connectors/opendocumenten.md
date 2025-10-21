# Opendocumenten 

## Configuration

The configuration properties of opendocumenten are:
- **host**: Base URL 
- **specificationUri**: The specification uri (could be a file or a url)
- **clientId**: The token to use for authentication
- **clientSecret**: The secret to use for authentication

## Endpoints

Has the following endpoints:
- enkelvoudiginformatieobject_list
- enkelvoudiginformatieobject_read

Other endpoints can be found by inspecting the specification.

## Connector Code

Copy the connector code down below and replace the `REFERENCE` with the refernce of the connector.`

```yaml
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.enkelvoudiginformatieobject_list" 
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.enkelvoudiginformatieobject_list"
          steps:
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "auteur|beschrijving|bronorganisatie|creatiedatum__gte|creatiedatum__lte|expand|identificatie|informatieobjecttype|locked|objectinformatieobjecten__object|objectinformatieobjecten__objectType|ordering|page|pageSize|titel|trefwoorden|trefwoorden__overlap|vertrouwelijkheidaanduiding"
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.enkelvoudiginformatieobject_read"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.enkelvoudiginformatieobject_read"
          steps:
              - setHeader:
                    name: "uuid"
                    variable: "id"
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "expand|uuid|registratieOp|versie"
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
                        def signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(variable.configProperties.clientSecret.getBytes());

                        def jwt = io.jsonwebtoken.Jwts.builder()
                              .issuer(variable.configProperties.clientId)
                              .issuedAt(new Date())
                              .claim("client_id", variable.configProperties.clientId)
                              .signWith(signingKey, io.jsonwebtoken.SignatureAlgorithm.HS256)
                              .compact();

                        exchange.in.setHeader("Authorization", "Bearer ${jwt}");
              - toD:
                    uri: "language:groovy:\"rest-openapi:${variable.configProperties.specificationUri}#${variable.operation}?host=${variable.configProperties.host}\""
              - unmarshal:
                    json: {}
```