import { defineConfig, devices } from "@playwright/test";
import { APP_BASE_URL, STORAGE_STATE } from "./fixtures/env";

/**
 * Playwright configuration for the IKO e2e suite.
 *
 * The full stack (app + Postgres + Redis + Keycloak) is owned by
 * `docker-compose-e2e.yaml`; CI brings it up and gates on the actuator readiness
 * probe before invoking `playwright test`. No `webServer` block is configured, so
 * the runner always targets an already-running stack at `baseURL`.
 *
 * The `setup` project logs in once against live Keycloak and saves the
 * authenticated storageState; the browser projects depend on it and reuse the
 * session via `storageState`. The smoke login spec drives login/logout itself
 * and therefore runs without a saved storageState.
 */
export default defineConfig({
    testDir: "tests",
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    workers: process.env.CI ? 1 : undefined,
    reporter: [["html", { outputFolder: "playwright-report", open: "never" }]],
    use: {
        baseURL: APP_BASE_URL,
        trace: "on-first-retry",
        screenshot: "on",
        video: "retain-on-failure",
    },
    projects: [
        {
            name: "setup",
            testMatch: /auth\.setup\.ts/,
            use: { ...devices["Desktop Chrome"] },
        },
        {
            // Smoke login/logout spec — drives the auth flow directly, no saved state.
            name: "smoke",
            testMatch: /admin\/login\.spec\.ts/,
            use: { ...devices["Desktop Chrome"] },
        },
        {
            // Authenticated Admin UI feature specs (connectors, adp) reuse the
            // saved session. Login smoke and the headless API specs are excluded so
            // they do not run pre-authenticated in a browser.
            name: "chromium",
            testMatch: /admin\/.*\.spec\.ts/,
            testIgnore: [/admin\/login\.spec\.ts/],
            use: {
                ...devices["Desktop Chrome"],
                storageState: STORAGE_STATE,
            },
            dependencies: ["setup"],
        },
        {
            // Public REST API specs — pure HTTP, no browser/session. Acquire a JWT
            // via the password-grant fixture (fixtures/api-token.ts) and hit the
            // secured `/endpoints` and `/aggregated-data-profiles` routes.
            name: "api",
            testMatch: /api\/.*\.spec\.ts/,
        },
    ],
});
