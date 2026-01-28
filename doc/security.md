# Security

## Authenticatie en Autorisatie

Binnen IKO worden twee vormen van authenticatie gebruikt:

1. **OAuth2 login flow** voor toegang tot het adminpaneel. Deze sessie wordt onderhouden via een `JSESSIONID`.
2. **JWT-token authenticatie** voor toegang tot API-endpoints zoals `/endpoints/**` en `/aggregated-data-profiles/**`.

### Security Filter Chains

IKO configures three ordered Spring Security filter chains in `SecurityConfig.kt`:

| Priority | Scope | Auth Method | Session |
|---|---|---|---|
| 1 (highest) | `/actuator/**` | JWT resource server | Stateless |
| 2 | `/endpoints/**`, `/aggregated-data-profiles/**` | JWT bearer token | Stateless |
| 3 (lowest) | `/admin/**`, `/login/**`, `/oauth2/**`, `/logout/**` | OAuth2/OIDC (Keycloak) | Session-based |

### JWT Token Structuur

Beide methodes maken gebruik van tokens waarin gebruikersrollen zijn opgenomen. Een geldig token bevat een `roles` claim die de toegestane gebruikersrollen weergeeft.

Voor toegang tot het adminpaneel is bijvoorbeeld de rol `ROLE_ADMIN` vereist.

```json
{
  "...": "...",
  "resource_access": {
    "iko": {
      "roles": ["ROLE_ADMIN", "ROLE_ENDPOINT_X", "ROLE_USER"]
    }
  }
}
```

JWT configuration details:
- **Authority prefix**: empty (no `ROLE_` prefix added by Spring; roles in the token already include it)
- **Authorities claim path**: `[resource_access][iko][roles]`
- **Audience**: `iko`
- **Issuer URI**: Configured via environment variable (Keycloak realm URL)

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
          authorities-claim-name: "[resource_access][iko][roles]"
```

## Endpointbeveiliging

### Publiek toegankelijke endpoints

Deze endpoints zijn vrij toegankelijk en vereisen geen authenticatie:

```
/logout/**
/login/**
/oauth2/**
/actuator/health
/actuator/info
```

### Adminpaneel

Deze endpoints zijn beveiligd via OAuth2 login. Alleen gebruikers met `ROLE_ADMIN` kunnen deze bereiken:

```
/admin/**
```

### Actuator Endpoints

Beveiligd via JWT. Health en info zijn publiek, overige (zoals Prometheus) vereisen `ROLE_ADMIN`:

```
/actuator/health     → publiek
/actuator/info       → publiek
/actuator/prometheus → vereist: ROLE_ADMIN
```

### Beveiligde API Endpoints (JWT)

Deze endpoints vereisen een geldig JWT-token met de juiste rol in te stellen op ADP:

```
/endpoints/**                → vereist: ENDPOINT ROLE
/aggregated-data-profiles/** → vereist: ADP ROLE
```

Standaard worden de rollen voor een aggregatieprofiel in het profiel zelf ingesteld. Dan moet het JWT-token deze rollen bevatten om erbij te kunnen.

#### Voorbeelden:

- `/endpoints/voorbeeld` → `ROLE_ENDPOINT_VOORBEELD`
- `/aggregated-data-profiles/voorbeeld?id=121` → `ROLE_USER`
- `/aggregated-data-profiles/uitzondering?id=2` met andere rol `ROLE_ADMIN`

## Encryption at Rest

Connector instance configuration values (hosts, tokens, client secrets) are encrypted in the database using AES-GCM:

- **Service**: `AesGcmEncryptionService`
- **JPA integration**: `AesGcmStringAttributeConverter` transparently encrypts on write and decrypts on read
- **Key**: Base64-encoded AES-256 key provided via `IKO_CRYPTO_KEY` environment variable
- **Scope**: Only `connector_instance_config` values are encrypted; other columns use plaintext

## Async Security Context

IKO uses a `DelegatingSecurityContextAsyncTaskExecutor` (50-thread pool) to propagate the security context into async operations, ensuring that background tasks retain the authenticated user's authorities.