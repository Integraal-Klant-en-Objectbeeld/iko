import { expect, Page } from "@playwright/test";
import { APP_BASE_URL } from "../fixtures/env";

/**
 * Page Object for the Admin UI authentication flow.
 *
 * Covers the Keycloak login form (`#username`, `#password`, `#kc-login`) and the
 * post-login `/admin` landing page + JS-driven logout (`#logout-link` → POST /logout).
 */
export class LoginPage {
    constructor(private readonly page: Page) {}

    /** Navigate to `/admin`; unauthenticated visits redirect to the Keycloak login form. */
    async goto(): Promise<void> {
        await this.page.goto(`${APP_BASE_URL}/admin`);
    }

    /** True when the current page is the Keycloak login form. */
    async isOnKeycloakLogin(): Promise<boolean> {
        return (await this.page.locator("#username").count()) > 0;
    }

    /** Fill and submit the Keycloak login form, then wait for the `/admin` landing page. */
    async loginAs(user: string, password: string): Promise<void> {
        await this.page.waitForSelector("#username");
        await this.page.locator("#username").fill(user);
        await this.page.locator("#password").fill(password);
        await Promise.all([
            this.page.waitForURL(`${APP_BASE_URL}/admin**`),
            this.page.locator("#kc-login").click(),
        ]);
        await expect(this.page.locator("#logout-link")).toBeVisible();
    }

    /** Click `#logout-link`; admin-ui.js builds a POST form to /logout with the CSRF token. */
    async logout(): Promise<void> {
        await Promise.all([
            this.page.waitForURL(/\/(admin|auth\/realms)/),
            this.page.locator("#logout-link").click(),
        ]);
    }
}
