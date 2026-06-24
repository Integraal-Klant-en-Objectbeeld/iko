import { test as base } from "@playwright/test";
import { ADMIN_USER, STORAGE_STATE } from "./env";
import { LoginPage } from "../pages/LoginPage";

/**
 * Setup helper used by the `setup` project in playwright.config.ts.
 *
 * Performs the real Keycloak login form flow once as `admin` and persists the
 * resulting session (cookies/storage) to `.auth/admin.json` so the dependent
 * projects can reuse the authenticated context without logging in per test.
 */
export async function globalLoginAndSaveState(
    loginPage: LoginPage,
    save: (path: string) => Promise<unknown>,
): Promise<void> {
    await loginPage.goto();
    await loginPage.loginAs(ADMIN_USER.username, ADMIN_USER.password);
    await save(STORAGE_STATE);
}

/**
 * Fixture that exposes a ready `LoginPage` for specs that drive login/logout
 * explicitly (the smoke spec). Most feature specs instead rely on the
 * pre-saved `storageState` and never touch this fixture.
 */
export const test = base.extend<{ loginPage: LoginPage }>({
    loginPage: async ({ page }, use) => {
        await use(new LoginPage(page));
    },
});

export { expect } from "@playwright/test";
