# Integraal Klant & Objectbeeld (IKO)

## Docker

The shipped Dockerfile, builds and creates an image that can be used to run IKO. The docker compose file contains some
services that IKO can connect to and setups a Keycloak authentication. Simply run `docker compose up -d` to run it.

To be able to login with Keycloak into the admin panel you will have to add the following to your respective host file.

```text
127.0.0.1 keycloak
```

## Development info 

### Source routes

