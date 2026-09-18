import { expect, fetchAccessToken, test, WRONG_ROLE_USER } from "../../fixtures/api-token";
import { APP_BASE_URL } from "../../fixtures/env";

/**
 * Public REST API: `/endpoints/{connector}/{config}/{operation}`.
 *
 * Drives the seeded `e2e-brp` connector (instance `e2e-brp-instance`, operation
 * `Personen`) which POSTs a RaadpleegMetBurgerservicenummer search to the BRP
 * personen mock via Camel `rest-openapi`. The mock answers any valid query with
 * an HTTP 200 `{ "type", "personen" }` envelope, so we assert on the envelope
 * shape rather than a specific seeded BSN.
 *
 * Positive: bearer token whose `realm_access.roles` includes ROLE_ADMIN passes
 * both the connector_endpoint_role check and reaches the mock.
 * Negative: no token -> 401; the route is unreachable without authentication.
 */

const ENDPOINT_PATH = "/endpoints/e2e-brp/e2e-brp-instance/Personen";

test.describe("Public API: /endpoints", () => {
    test("authenticated call returns 200 with a payload from the BRP mock", async ({
        apiContext,
    }) => {
        const res = await apiContext.get(ENDPOINT_PATH);
        expect(res.status(), await res.text()).toBe(200);

        const body = await res.json();
        // The personen-mock envelope: a `type` discriminator and a `personen` array.
        expect(body).toHaveProperty("type", "RaadpleegMetBurgerservicenummer");
        expect(Array.isArray(body.personen)).toBe(true);
    });

    test("unauthenticated call is rejected with 401", async ({
        anonymousApiContext,
    }) => {
        const res = await anonymousApiContext.get(ENDPOINT_PATH);
        expect(res.status()).toBe(401);
    });

    test("a malformed bearer token is rejected with 401", async ({
        playwright,
    }) => {
        const context = await playwright.request.newContext({
            baseURL: APP_BASE_URL,
            extraHTTPHeaders: { Authorization: "Bearer not-a-real-token" },
        });
        const res = await context.get(ENDPOINT_PATH);
        expect(res.status()).toBe(401);
        await context.dispose();
    });

    test("an authenticated user lacking ROLE_ADMIN is rejected (no endpoint role)", async ({
        playwright,
    }) => {
        // `user` authenticates fine but holds only ROLE_USER, so it fails the
        // connector_endpoint_role (ROLE_ADMIN) check inside Camel. The global error
        // handler maps ConnectorAccessDenied -> 401.
        const tokenContext = await playwright.request.newContext();
        const token = await fetchAccessToken(tokenContext, WRONG_ROLE_USER);
        await tokenContext.dispose();

        const context = await playwright.request.newContext({
            baseURL: APP_BASE_URL,
            extraHTTPHeaders: { Authorization: `Bearer ${token}` },
        });
        const res = await context.get(ENDPOINT_PATH);
        expect(res.status(), await res.text()).toBe(401);
        await context.dispose();
    });
});
