# Integraal Klant & Objectbeeld (IKO)

## Docker

The shipped Dockerfile, builds and creates an image that can be used to run IKO. The docker compose file contains some
services that IKO can connect to and setups a Keycloak authentication. Simply run `docker compose up -d` to run it.

To be able to login with Keycloak into the admin panel you will have to add the following to your respective host file.

```text
127.0.0.1 keycloak
```

### Publishing
Choos the platform to use in the build --platform=linux/amd64|linux/arm64
```text
docker build --platform=linux/amd64 .
```

## Development info 

The env.template file contains a SPRING_THYMELEAF_PREFIX=file:src/main/resources/templates/
This is to allow local dev to have no caching when working on HTML. Remove it when running the docker container.

### Source routes

## More documentation

You can find more documentation [here](./doc/index.md)