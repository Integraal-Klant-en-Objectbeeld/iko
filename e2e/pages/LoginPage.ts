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

    /** Open the header switcher panel and click `#logout-link`; admin-ui.js builds a POST form to /logout with the CSRF token. */
    async logout(): Promise<void> {
        // #logout-link lives inside the collapsed cds-header-panel#switcher-panel
        // (positioned off-canvas until expanded). The toggle action sets
        // expanded="true" on the panel, but Carbon hydration can drop an early
        // click — so poll: click the toggle only while the panel is collapsed,
        // and wait until it reports expanded before clicking logout. Clicking
        // only when collapsed avoids toggling it back closed.
        const panel = this.page.locator("#switcher-panel");
        const toggle = this.page.locator(
            'cds-header-global-action[panel-id="switcher-panel"]',
        );
        await expect(async () => {
            if ((await panel.getAttribute("expanded")) !== "true") {
                await toggle.click();
            }
            await expect(panel).toHaveAttribute("expanded", "true", {
                timeout: 1000,
            });
        }).toPass({ timeout: 15000 });
        await this.page.locator("#logout-link").click();
        // Logout is a multi-hop chain: POST /logout -> Keycloak end-session ->
        // post-logout redirect to /admin -> unauthenticated -> Keycloak login
        // form. Wait for the Keycloak login form (#username) — the definitive
        // logged-out state — rather than racing intermediate URLs.
        await this.page.waitForSelector("#username");
    }
}
