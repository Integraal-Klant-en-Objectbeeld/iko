# Security

All endpoints and admin pages are protected.

## Admin pages

The admin pages are protected by keycloak / OIDC.

## Endpoint

Each endpoint is protected by a custom role `ROLE_ENDPOINT_<ENDPOINT_NAME>` where `ENDPOINT_NAME` is the search name of the endpoint
where all non-ascii characters are replaced by `_`. 

**DEV**: To give access to a new endpoint you will need to create a new realm role in keycloak and add it to the `iko` client.
``
## Aggregated Data Profile's

Each profile is protected by a custom role `ROLE_AGGREGATED_DATA_PROFILE_<AGGREGATED_DATA_PROFILE_NAME>` where `AGGREGATED_DATA_PROFILE_NAME` is the name of the 
ADG where all non-ascii characters are replaced by `_`.

**DEV**: To give access to a new search you will need to create a new realm role in keycloak and add it to the `iko` client.
