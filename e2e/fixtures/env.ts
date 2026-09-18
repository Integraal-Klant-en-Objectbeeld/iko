/**
 * Central configuration for the e2e suite.
 *
 * Values default to the local `docker-compose-e2e.yaml` published ports and the
 * `valtimo` Keycloak realm, and can be overridden via environment variables for CI.
 */

export const APP_BASE_URL = process.env.E2E_BASE_URL ?? "http://localhost:8080";

export const KEYCLOAK_BASE_URL =
    process.env.E2E_KEYCLOAK_BASE_URL ?? "http://localhost:8082";

export const KEYCLOAK_REALM = process.env.E2E_KEYCLOAK_REALM ?? "valtimo";

export const KEYCLOAK_ISSUER = `${KEYCLOAK_BASE_URL}/auth/realms/${KEYCLOAK_REALM}`;

export const KEYCLOAK_TOKEN_URL = `${KEYCLOAK_ISSUER}/protocol/openid-connect/token`;

export const OIDC_CLIENT_ID = process.env.E2E_CLIENT_ID ?? "iko";

export const OIDC_CLIENT_SECRET =
    process.env.E2E_CLIENT_SECRET ?? "ThisIsNotASecret";

/**
 * Default browser-login user — seeded in the `valtimo` realm with ROLE_ADMIN.
 * The header renders the OIDC `name` claim (full name) when present, otherwise
 * the email; the `iko` client maps both into userinfo for this user.
 */
export const ADMIN_USER = {
    username: process.env.E2E_ADMIN_USERNAME ?? "admin",
    password: process.env.E2E_ADMIN_PASSWORD ?? "admin",
    email: "admin@example.com",
    fullName: "Asha Miller",
};

/** Where the authenticated browser storageState is persisted by the setup project. */
export const STORAGE_STATE = ".auth/admin.json";
