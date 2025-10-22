# Openzaak

## Configuration

The configuration properties of openzaak are:
- **host**: Base URL 
- **specificationUri**: The specification uri (could be a file or a url)
- **clientId**: The token to use for authentication
- **clientSecret**: The secret to use for authentication

## Endpoints

Openzaak has the following endpoints:
- zaak_list
- zaak_read
- zaakinformatieobject_list
- zaakinformatieobject_read

Other endpoints can be found by inspecting the specification.

## Connector Code

Copy the connector code down below and replace the `REFERENCE` with the refernce of the connector.`

```yaml
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.zaakinformatieobject_read"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.zaakinformatieobject_read"
          steps:
              - setHeader:
                    name: "uuid"
                    variable: "id"
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "uuid"
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.zaakinformatieobject_list"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.zaakinformatieobject_list"
          steps:
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "informatieobject|zaak"
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.zaak_list" 
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.zaak_list"
          steps:
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "archiefactiedatum|archiefactiedatum__gt|archiefactiedatum__isnull|archiefactiedatum__lt|archiefnominatie|archiefnominatie__in|archiefstatus|archiefstatus__in|bronorganisatie|bronorganisatie__in|einddatum|einddatumGepland|einddatumGepland__gt|einddatumGepland__lt|einddatum__gt|einddatum__isnull|einddatum__lt|expand|identificatie|maximaleVertrouwelijkheidaanduiding|ordering|page|registratiedatum|registratiedatum__gt|registratiedatum__lt|rol__betrokkene|rol__betrokkeneIdentificatie__medewerker__identificatie|rol__betrokkeneIdentificatie__natuurlijkPersoon__anpIdentificatie|rol__betrokkeneIdentificatie__natuurlijkPersoon__inpA_nummer|rol__betrokkeneIdentificatie__natuurlijkPersoon__inpBsn|rol__betrokkeneIdentificatie__nietNatuurlijkPersoon__annIdentificatie|rol__betrokkeneIdentificatie__nietNatuurlijkPersoon__innNnpId|rol__betrokkeneIdentificatie__organisatorischeEenheid__identificatie|rol__betrokkeneIdentificatie__vestiging__vestigingsNummer|rol__betrokkeneType|rol__omschrijvingGeneriek|startdatum|startdatum__gt|startdatum__gte|startdatum__lt|startdatum__lte|uiterlijkeEinddatumAfdoening|uiterlijkeEinddatumAfdoening__gt|uiterlijkeEinddatumAfdoening__lt|zaaktype"
              - setHeader:
                    name: "Accept-Crs"
                    constant: "EPSG:4326"
- route:
      id: "direct:iko:endpoint:transform:REFERENCE.zaak_read"
      errorHandler:
          noErrorHandler: {}
      from:
          uri: "direct:iko:endpoint:transform:REFERENCE.zaak_read"
          steps:
              - setHeader:
                    name: "uuid"
                    variable: "id"
              - removeHeaders:
                    pattern: "*"
                    excludePattern: "expand|uuid"
              - setHeader:
                    name: "Accept-Crs"
                    constant: "EPSG:4326"
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