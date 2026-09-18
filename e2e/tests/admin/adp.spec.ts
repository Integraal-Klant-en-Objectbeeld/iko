import { expect, test } from "@playwright/test";
import { AdpListPage } from "../../pages/AdpListPage";
import { AdpFormPage, AdpEditForm } from "../../pages/AdpFormPage";

/**
 * Aggregated Data Profiles Admin UI coverage: add / edit / search / paginate.
 *
 * Runs in the `chromium` project, which loads the saved `storageState`, so every
 * test starts already authenticated as `admin`.
 *
 * Baseline `e2e-adp-*` profiles are provided by the mounted Flyway seed
 * (e2e/seed/V9999.01.01.1__e2e_baseline.sql) so search/pagination assertions hold
 * regardless of test order or which create specs ran first. They reference the
 * single seeded connector instance/endpoint (e2e-instance-01 / e2e-endpoint-01)
 * that the Add form pre-selects. The add/edit tests use a per-run-unique suffix so
 * reruns against a persistent DB do not collide with rows from an earlier run.
 *
 * The list page uses the identical shared selectors as the Connectors page
 * (#profile-table, #search-bar, #search-results, #active-filter-toggle,
 * #pagination-container) — AdpListPage mirrors ConnectorListPage exactly.
 */

const runId = Date.now().toString(36);

test.describe("Aggregated Data Profiles Admin UI", () => {
    test("baseline profiles are seeded and listed", async ({ page }) => {
        const list = new AdpListPage(page);
        await list.goto();
        // The mounted seed inserted >= one page (10) of e2e-adp-* profiles.
        expect(await list.rowCount()).toBeGreaterThan(0);
        await list.searchFor("e2e-adp-01");
        expect(await list.hasRowNamed("e2e-adp-01")).toBe(true);
    });

    test("add a profile via the modal; it appears in the list", async ({
        page,
    }) => {
        const list = new AdpListPage(page);
        const form = new AdpFormPage(page);
        const name = `e2e-adp-created-${runId}`;

        await list.goto();
        await list.openAddModal();
        // Name + roles only; the instance/endpoint selects default to the single
        // seeded e2e instance/endpoint, and the JQ editors ship valid defaults.
        await form.fillAdd({ name, roles: "ROLE_ADMIN" });
        await form.submitCreate();

        // Create pushes the URL to the new profile's detail page.
        await expect(page).toHaveURL(
            /\/admin\/aggregated-data-profiles\/[0-9a-f-]+/,
        );

        // Back on the list it is findable via search.
        await list.goto();
        await list.searchFor(name);
        expect(await list.hasRowNamed(name)).toBe(true);
    });

    test("edit persists: change roles on a profile and reload", async ({
        page,
    }) => {
        const list = new AdpListPage(page);
        const form = new AdpFormPage(page);
        const edit = new AdpEditForm(page);
        const name = `e2e-adp-edit-${runId}`;
        const newRoles = "ROLE_ADMIN,ROLE_USER";

        // Create a fresh DRAFT profile to edit (FINAL profiles are immutable).
        await list.goto();
        await list.openAddModal();
        await form.fillAdd({ name, roles: "ROLE_ADMIN" });
        await form.submitCreate();
        await expect(page).toHaveURL(
            /\/admin\/aggregated-data-profiles\/[0-9a-f-]+/,
        );
        const detailUrl = page.url();

        // Edit the roles on the General tab and save.
        await edit.setRoles(newRoles);
        await edit.submit();

        // Reload the detail page; the new roles value must still be there.
        await page.goto(detailUrl);
        await expect(
            page.locator("#profile-edit-form cds-text-input[name='roles']"),
        ).toHaveAttribute("value", newRoles);
    });

    test("search filters the results", async ({ page }) => {
        const list = new AdpListPage(page);
        await list.goto();

        await list.searchFor("e2e-adp-02");
        expect(await list.hasRowNamed("e2e-adp-02")).toBe(true);
        // A specific seeded name must not bring back unrelated baseline rows.
        expect(await list.hasRowNamed("e2e-adp-11")).toBe(false);

        await list.searchFor("no-such-profile-xyz");
        await expect(
            list.results.locator("cds-table-row", {
                hasText: "No results found",
            }),
        ).toHaveCount(1);
    });

    test("paginate across the baseline rows", async ({ page }) => {
        const list = new AdpListPage(page);
        await list.goto();

        // Restrict to the deterministic baseline set so page contents are stable
        // regardless of any rows other specs created.
        await list.searchFor("e2e-adp-");

        const firstPageRows = await list.rows.allInnerTexts();
        expect(firstPageRows.length).toBeGreaterThan(0);
        // Page 1 (sorted by name asc) starts at e2e-adp-01.
        expect(await list.hasRowNamed("e2e-adp-01")).toBe(true);

        await list.goToPage(2);
        // Page 2 shows later names and no longer the first one.
        expect(await list.hasRowNamed("e2e-adp-11")).toBe(true);
        expect(await list.hasRowNamed("e2e-adp-01")).toBe(false);
    });
});
