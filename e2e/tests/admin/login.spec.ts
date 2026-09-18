import { test, expect } from "../../fixtures/auth";
import { ADMIN_USER } from "../../fixtures/env";

/**
 * Smoke coverage for the Admin UI OAuth2/OIDC flow against live Keycloak:
 *  - unauthenticated /admin redirects to the Keycloak login form;
 *  - login as `admin` lands on /admin with the user's name/email rendered;
 *  - logout leaves the authenticated landing page.
 *
 * This spec runs in the `smoke` project without a saved storageState so it
 * exercises the real login form every run.
 */

test.describe("Admin UI login/logout", () => {
    test("unauthenticated /admin redirects to Keycloak login form", async ({
        loginPage,
    }) => {
        await loginPage.goto();
        expect(await loginPage.isOnKeycloakLogin()).toBe(true);
    });

    test("admin can log in and reach the /admin landing page", async ({
        loginPage,
        page,
    }) => {
        await loginPage.goto();
        await loginPage.loginAs(ADMIN_USER.username, ADMIN_USER.password);

        await expect(page).toHaveURL(/\/admin/);
        await expect(page.locator("#logout-link")).toBeVisible();
        // The OIDC userinfo identity (full name from the `name` claim) is rendered
        // in the header on the landing page.
        await expect(page.locator(".header-user-name")).toContainText(
            ADMIN_USER.fullName,
        );
    });

    test("admin can log out", async ({ loginPage, page }) => {
        await loginPage.goto();
        await loginPage.loginAs(ADMIN_USER.username, ADMIN_USER.password);
        await loginPage.logout();

        // After logout the authenticated landing is no longer shown: either the
        // Keycloak login form, or /admin re-prompting auth.
        await expect(page.locator("#logout-link")).toHaveCount(0);
    });
});
