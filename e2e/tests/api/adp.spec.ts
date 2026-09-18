import { expect, fetchAccessToken, test, WRONG_ROLE_USER } from "../../fixtures/api-token";
import { APP_BASE_URL } from "../../fixtures/env";

/**
 * Public REST API: `/aggregated-data-profiles/{name}`.
 *
 * The seeded `e2e-brp-personen` profile (roles=ROLE_ADMIN) aggregates the single
 * `e2e-brp` BRP endpoint and passes the mock envelope through unchanged ('.').
 * A 200 here proves the full path: JWT -> AuthRoute role check -> connector route
 * -> BRP mock -> aggregation -> response.
 *
 * Negative: unauthenticated -> 401; authenticated-without-ROLE_ADMIN -> 401
 * (AggregatedDataProfileAccessDenied is mapped to 401 by the global error handler).
 */

const ADP_PATH = "/aggregated-data-profiles/e2e-brp-personen";

test.describe("Public API: /aggregated-data-profiles", () => {
    test("authenticated call returns 200 with data sourced from the BRP mock", async ({
        apiContext,
    }) => {
        const res = await apiContext.get(ADP_PATH);
        expect(res.status(), await res.text()).toBe(200);

        // The result transform is '.', so the aggregated payload is the mock's
        // own `{ type, personen }` envelope, surfaced under the ADP left/right shape.
        const body = await res.json();
        const serialized = JSON.stringify(body);
        expect(serialized).toContain("RaadpleegMetBurgerservicenummer");
        expect(serialized).toContain("personen");
    });

    test("unauthenticated call is rejected with 401", async ({
        anonymousApiContext,
    }) => {
        const res = await anonymousApiContext.get(ADP_PATH);
        expect(res.status()).toBe(401);
    });

    test("an authenticated user lacking ROLE_ADMIN is rejected", async ({
        playwright,
    }) => {
        const tokenContext = await playwright.request.newContext();
        const token = await fetchAccessToken(tokenContext, WRONG_ROLE_USER);
        await tokenContext.dispose();

        const context = await playwright.request.newContext({
            baseURL: APP_BASE_URL,
            extraHTTPHeaders: { Authorization: `Bearer ${token}` },
        });
        const res = await context.get(ADP_PATH);
        expect(res.status(), await res.text()).toBe(401);
        await context.dispose();
    });
});
