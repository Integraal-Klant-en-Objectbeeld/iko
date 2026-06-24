import { expect, test } from "@playwright/test";
import { ConnectorListPage } from "../../pages/ConnectorListPage";
import { ConnectorFormPage } from "../../pages/ConnectorFormPage";

/**
 * Connectors Admin UI coverage: add / edit / search / paginate.
 *
 * Runs in the `chromium` project, which loads the saved `storageState`, so every
 * test starts already authenticated as `admin`.
 *
 * Baseline `e2e-conn-*` connectors are provided by the mounted Flyway seed
 * (e2e/seed/V9999.01.01.1__e2e_baseline.sql) so search/pagination assertions hold
 * regardless of test order or which create specs ran first. The add/edit tests
 * use a per-run-unique suffix so reruns against a persistent DB do not collide
 * with rows created by an earlier run.
 */

const runId = Date.now().toString(36);

test.describe("Connectors Admin UI", () => {
    test("baseline connectors are seeded and listed", async ({ page }) => {
        const list = new ConnectorListPage(page);
        await list.goto();
        // The mounted seed inserted >= one page (10) of e2e-conn-* connectors.
        expect(await list.rowCount()).toBeGreaterThan(0);
        await list.searchFor("e2e-conn-01");
        expect(await list.hasRowNamed("e2e-conn-01")).toBe(true);
    });

    test("add a connector via the modal; it appears in the list", async ({
        page,
    }) => {
        const list = new ConnectorListPage(page);
        const form = new ConnectorFormPage(page);
        const name = `e2e-created-${runId}`;

        await list.goto();
        await list.openAddModal();
        await form.fillConnector({ name, reference: name });
        await form.submit();

        // Create pushes the URL to the new connector's detail page.
        await expect(page).toHaveURL(/\/admin\/connectors\/[0-9a-f-]+/);

        // Back on the list it is findable via search.
        await list.goto();
        await list.searchFor(name);
        expect(await list.hasRowNamed(name)).toBe(true);
    });

    test("edit persists: add an instance to a connector and reload", async ({
        page,
    }) => {
        const list = new ConnectorListPage(page);
        const form = new ConnectorFormPage(page);
        const connectorName = `e2e-edit-${runId}`;
        const instanceName = `e2e-instance-${runId}`;

        // Create a fresh (DRAFT) connector to edit.
        await list.goto();
        await list.openAddModal();
        await form.fillConnector({ name: connectorName, reference: connectorName });
        await form.submit();
        await expect(page).toHaveURL(/\/admin\/connectors\/[0-9a-f-]+/);
        const detailUrl = page.url();

        // Add an instance (plain text fields) via the instance modal.
        await page.locator("#instance-table cds-button[kind='primary']").click();
        await expect(page.locator("#active-modal")).toBeVisible();
        await form.fillInstance({
            name: instanceName,
            reference: instanceName,
            apiSpecificationUrl: "https://example.com/openapi.yaml",
        });
        await form.submit();

        // Reload the connector detail page; the instance must still be there.
        await page.goto(detailUrl);
        await expect(
            page.locator("#instance-table cds-table-row", {
                hasText: instanceName,
            }),
        ).toHaveCount(1);
    });

    test("search filters the results", async ({ page }) => {
        const list = new ConnectorListPage(page);
        await list.goto();

        await list.searchFor("e2e-conn-02");
        expect(await list.hasRowNamed("e2e-conn-02")).toBe(true);
        // A specific seeded name must not bring back unrelated baseline rows.
        expect(await list.hasRowNamed("e2e-conn-11")).toBe(false);

        await list.searchFor("no-such-connector-xyz");
        await expect(
            list.results.locator("cds-table-row", { hasText: "No result found" }),
        ).toHaveCount(1);
    });

    test("paginate across the baseline rows", async ({ page }) => {
        const list = new ConnectorListPage(page);
        await list.goto();

        // Restrict to the deterministic baseline set so page contents are stable
        // regardless of any rows other specs created.
        await list.searchFor("e2e-conn-");

        const firstPageRows = await list.rows.allInnerTexts();
        expect(firstPageRows.length).toBeGreaterThan(0);
        // Page 1 (sorted by name asc) starts at e2e-conn-01.
        expect(await list.hasRowNamed("e2e-conn-01")).toBe(true);

        await list.goToPage(2);
        // Page 2 shows later names and no longer the first one.
        expect(await list.hasRowNamed("e2e-conn-11")).toBe(true);
        expect(await list.hasRowNamed("e2e-conn-01")).toBe(false);
    });
});
