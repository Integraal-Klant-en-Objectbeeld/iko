# Security

## Authenticatie en Autorisatie

Binnen IKO worden twee vormen van authenticatie gebruikt:

1. **OAuth2 login flow** voor toegang tot het adminpaneel. Deze sessie wordt onderhouden via een `JSESSIONID`.
2. **JWT-token authenticatie** voor toegang tot API-endpoints zoals `/endpoints/**` en `/aggregated-data-profiles/**`.

### JWT Token Structuur

Beide methodes maken gebruik van tokens waarin gebruikersrollen zijn opgenomen. Een geldig token bevat een `roles` claim die de toegestane gebruikersrollen weergeeft.

Voor toegang tot het adminpaneel is bijvoorbeeld de rol `ROLE_ADMIN` vereist.

```json
{
  "...": "...",
  "roles": ["ROLE_ADMIN", "ROLE_ENDPOINT_X", "..."]
}
```

### Spring Security OAuth2 Configuratie

Hieronder staat een voorbeeld van de configuratie zoals die wordt gebruikt binnen de Docker Compose setup van IKO:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            provider: keycloak
            authorization-grant-type: authorization_code
            scope: openid
            client-id: ${CLIENT_ID}
            client-secret: ${CLIENT_SECRET}
        provider:
          keycloak:
            issuer-uri: ${ISSUER_URI}
      resourceserver:
        jwt:
          issuer-uri: ${ISSUER_URI}
          audiences: iko
          authority-prefix: ""
          authorities-claim-name: roles
```

## Endpointbeveiliging

### Publiek toegankelijke endpoints

Deze endpoints zijn vrij toegankelijk en vereisen geen authenticatie:

```
/logout/**
/login/**
/oauth2/**
```

### Adminpaneel

Deze endpoints zijn beveiligd via OAuth2 login. Alleen gebruikers met `ROLE_ADMIN` kunnen deze bereiken:

```
/admin/**
```

### Beveiligde API Endpoints (JWT)

Deze endpoints vereisen een geldig JWT-token met de juiste rol:

```
/endpoints/**                → vereist: ROLE_ENDPOINT_{NAAM}
/aggregated-data-profiles/** → vereist: ROLE_AGGREGATED_DATA_PROFILE_{NAAM}
```

Voorbeelden:
- `/endpoints/voorbeeld` → `ROLE_ENDPOINT_VOORBEELD`
- `/aggregated-data-profiles/voorbeeld/121` → `ROLE_AGGREGATED_DATA_PROFILE_VOORBEELD`
``