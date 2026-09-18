import {
    test as base,
    request as playwrightRequest,
    type APIRequestContext,
} from "@playwright/test";
import {
    ADMIN_USER,
    APP_BASE_URL,
    KEYCLOAK_TOKEN_URL,
    OIDC_CLIENT_ID,
    OIDC_CLIENT_SECRET,
} from "./env";

/**
 * A seeded realm user that authenticates successfully but lacks ROLE_ADMIN
 * (`user` holds only ROLE_USER + default-roles-valtimo). Used by the API specs
 * to exercise the Camel per-endpoint / per-profile role check: a valid JWT that
 * does not carry the required authority is rejected by the route (mapped to 401
 * by the global error handler), not by the resource-server filter. The valtimo
 * demo realm uses username == password.
 */
export const WRONG_ROLE_USER = {
    username: process.env.E2E_WRONG_ROLE_USERNAME ?? "user",
    password: process.env.E2E_WRONG_ROLE_PASSWORD ?? "user",
};

/**
 * Acquire a bearer access token for the public REST API via the OAuth2 password
 * grant against the live Keycloak `valtimo` realm.
 *
 * Using a known user (`admin`) means the JWT carries the realm roles that the
 * e2e app maps to authorities (`SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUTHORITIESCLAIMNAME=[realm_access][roles]`,
 * audience `account`), which is what satisfies the Camel per-profile /
 * per-endpoint role checks for the seeded BRP data.
 */
export async function fetchAccessToken(
    request: APIRequestContext,
    user: { username: string; password: string } = ADMIN_USER,
): Promise<string> {
    const res = await request.post(KEYCLOAK_TOKEN_URL, {
        form: {
            grant_type: "password",
            client_id: OIDC_CLIENT_ID,
            client_secret: OIDC_CLIENT_SECRET,
            username: user.username,
            password: user.password,
        },
    });
    if (!res.ok()) {
        throw new Error(
            `Token request failed: ${res.status()} ${await res.text()}`,
        );
    }
    const body = (await res.json()) as { access_token?: string };
    if (!body.access_token) {
        throw new Error("Token response did not contain an access_token");
    }
    return body.access_token;
}

type ApiFixtures = {
    /** Raw token string, useful for negative/"wrong token" cases. */
    accessToken: string;
    /**
     * APIRequestContext pre-configured with `baseURL` = the app and the
     * `Authorization: Bearer <token>` header attached to every request.
     */
    apiContext: APIRequestContext;
    /** Unauthenticated APIRequestContext against the app (for 401 cases). */
    anonymousApiContext: APIRequestContext;
};

/**
 * API test base. Specs that need a bearer-auth'd context use the `apiContext`
 * fixture; `anonymousApiContext` covers the unauthenticated negative case.
 *
 * These specs do not touch the browser, so they ignore the saved storageState
 * and the `chromium` project entirely (they live under tests/api/).
 */
export const test = base.extend<ApiFixtures>({
    accessToken: async ({ playwright }, use) => {
        const tokenContext = await playwrightRequest.newContext();
        try {
            const token = await fetchAccessToken(tokenContext);
            await use(token);
        } finally {
            await tokenContext.dispose();
        }
    },
    apiContext: async ({ playwright, accessToken }, use) => {
        const context = await playwright.request.newContext({
            baseURL: APP_BASE_URL,
            extraHTTPHeaders: { Authorization: `Bearer ${accessToken}` },
        });
        await use(context);
        await context.dispose();
    },
    anonymousApiContext: async ({ playwright }, use) => {
        const context = await playwright.request.newContext({
            baseURL: APP_BASE_URL,
        });
        await use(context);
        await context.dispose();
    },
});

export { expect } from "@playwright/test";
