import { expect, Locator, Page } from "@playwright/test";
import { APP_BASE_URL } from "../fixtures/env";

/**
 * Page Object for the Aggregated Data Profiles list view
 * (`/admin/aggregated-data-profiles`).
 *
 * Anchors on the exact same stable host IDs as the Connectors list page
 * (`#profile-table`, `#search-bar`, `#search-results`, `#active-filter-toggle`,
 * `#pagination-container`) — `list.html` is shared between both flows, so the
 * selector contract is identical and intentionally mirrored from
 * `ConnectorListPage`. Only the `/filter` URL differs.
 *
 * HTMX wiring this POM cooperates with (see list.html):
 *  - `#search-bar` filters via `hx-trigger="input changed delay:600ms"`, swapping
 *    `#search-results` (outerHTML) plus an out-of-band `#pagination-container`.
 *  - `#pagination-container` reacts to `cds-pagination-changed-current`, swapping
 *    `#search-results`.
 * Because `#search-results` is replaced (outerHTML), helpers wait for the matching
 * `/filter` response and re-assert the expected post-condition rather than relying
 * on a fixed timeout.
 */
export class AdpListPage {
    readonly table: Locator;
    readonly searchBar: Locator;
    readonly results: Locator;
    readonly rows: Locator;
    readonly pagination: Locator;
    readonly addButton: Locator;

    constructor(private readonly page: Page) {
        this.table = page.locator("#profile-table");
        // The Carbon search host renders its <input> in Shadow DOM; target the
        // inner input so .fill() works (the host carries the HTMX wiring).
        this.searchBar = page.locator("#search-bar input");
        this.results = page.locator("#search-results");
        this.rows = page.locator(
            "#search-results cds-table-row.app--table-row-clickable",
        );
        this.pagination = page.locator("#pagination-container");
        // The list page only has one primary "Add new ..." button in the toolbar.
        this.addButton = page.locator("#profile-table cds-button[hx-target='#modal-container']");
    }

    /** Navigate to the ADP list and wait for the table to render. */
    async goto(): Promise<void> {
        await this.page.goto(`${APP_BASE_URL}/admin/aggregated-data-profiles`);
        await expect(this.table).toBeVisible();
        await expect(this.results).toBeAttached();
    }

    /** Number of clickable profile rows currently rendered. */
    async rowCount(): Promise<number> {
        return this.rows.count();
    }

    /** True when a clickable row whose name cell matches `name` is present. */
    async hasRowNamed(name: string): Promise<boolean> {
        return (
            (await this.rows.filter({ hasText: name }).count()) > 0
        );
    }

    /**
     * Type into the search bar and wait for the debounced (600ms) HTMX swap to
     * settle. We wait for the `/filter` response then re-attach to the swapped
     * results body.
     */
    async searchFor(term: string): Promise<void> {
        const swap = this.waitForFilterSwap();
        await this.searchBar.fill(term);
        await swap;
    }

    /** Clear the search bar and wait for the unfiltered list to come back. */
    async clearSearch(): Promise<void> {
        const swap = this.waitForFilterSwap();
        await this.searchBar.fill("");
        await swap;
    }

    /** Drive the pagination control to navigate to 1-based page `n`. */
    async goToPage(n: number): Promise<void> {
        const swap = this.waitForFilterSwap();
        // cds-pagination exposes a `page` property and emits
        // `cds-pagination-changed-current`; set the property and dispatch the
        // event the HTMX trigger listens for.
        await this.pagination.evaluate((el, page) => {
            (el as unknown as { page: number }).page = page;
            el.dispatchEvent(
                new CustomEvent("cds-pagination-changed-current", {
                    bubbles: true,
                    detail: { page },
                }),
            );
        }, n);
        await swap;
    }

    /** Open the detail page for the row whose name cell matches `name`. */
    async openRow(name: string): Promise<void> {
        await this.rows.filter({ hasText: name }).first().click();
        await this.page.waitForURL(
            /\/admin\/aggregated-data-profiles\/[0-9a-f-]+/,
        );
    }

    /**
     * Click "Add new 'Aggregated Data Profile'"; the create modal is swapped into
     * #modal-container.
     */
    async openAddModal(): Promise<void> {
        await this.addButton.click();
        await expect(this.page.locator("#active-modal")).toBeVisible();
    }

    /**
     * Await the `#search-results` outerHTML swap that follows a search/pagination
     * action. Resolves once the matching `/filter` response is observed.
     */
    private async waitForFilterSwap(): Promise<void> {
        await this.page.waitForResponse(
            (res) =>
                res
                    .url()
                    .includes("/admin/aggregated-data-profiles/filter") &&
                res.ok(),
        );
        await expect(this.results).toBeAttached();
    }
}
