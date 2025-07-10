# Security

All endpoints and admin pages are protected.

## Admin pages

The admin pages are protected by keycloak / OIDC.

## Searches

Each search is protected by a custom role `ROLE_SEARCH_<SEARCH_NAME>` where `SEARCH_NAME` is the search name of the search
where all non-ascii characters are replaced by `_`. 

**DEV**: To give access to a new search you will need to create a new realm role in keycloak and add it to the `iko` client.
``
## Profiles 

Each profile is protected by a custom role `ROLE_PROFILE_<PROFILE_NAME>` where `PROFILE_NAME` is the profile name of the 
profile where all non-ascii characters are replaced by `_`.

**DEV**: To give access to a new search you will need to create a new realm role in keycloak and add it to the `iko` client.
