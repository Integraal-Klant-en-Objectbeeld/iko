import { expect, Locator, Page } from "@playwright/test";
import { APP_BASE_URL } from "../fixtures/env";

/**
 * Page Object for the Aggregated Data Profile detail page
 * (`/admin/aggregated-data-profiles/{id}`), focused on the Relations tab and the
 * relation-delete confirmation flow (issue #324).
 *
 * Flow this POM drives (see detail-page.html / relations-panel.html /
 * relation/edit.html / relation/delete.html):
 *  - The Relations tab activates `#panel-relations`, which holds the relations tree.
 *  - Each relation is a `cds-tree-node[label='<propertyName>']`; a `mousedown`
 *    (fired by a normal click) triggers an HTMX GET that swaps the relation edit
 *    form into `#selected-relation`.
 *  - The edit form's danger button GETs the confirmation fragment into
 *    `#modal-container` (rendered as `<cds-modal id="active-modal">`); admin-ui.js
 *    then opens it via the `data-after-request-open-modal="#active-modal"` hook.
 */
export class AdpDetailPage {
    readonly relationsTab: Locator;
    readonly relationsPanel: Locator;
    readonly relationsTree: Locator;
    readonly selectedRelation: Locator;
    readonly deleteButton: Locator;
    readonly confirmModal: Locator;
    readonly cancelButton: Locator;

    constructor(private readonly page: Page) {
        // The Carbon tab activates only when its inner focusable is clicked, so
        // target it by ARIA role rather than the `cds-tab` host element.
        this.relationsTab = page.getByRole("tab", { name: "Relations" });
        this.relationsPanel = page.locator("#panel-relations");
        this.relationsTree = page.locator("#relations-tree");
        this.selectedRelation = page.locator("#selected-relation");
        this.deleteButton = page.locator(
            "#selected-relation cds-button[kind='danger']",
        );
        this.confirmModal = page.locator("#active-modal");
        this.cancelButton = this.confirmModal.locator(
            "cds-modal-footer-button[kind='secondary']",
        );
    }

    /** Navigate to the detail page and open the Relations tab. */
    async gotoRelations(id: string): Promise<void> {
        await this.page.goto(
            `${APP_BASE_URL}/admin/aggregated-data-profiles/${id}`,
        );
        await expect(this.relationsTab).toBeVisible();
        await this.relationsTab.click();
        await expect(this.relationsTab).toHaveAttribute("aria-selected", "true");
        await expect(this.relationsTree).toBeVisible();
    }

    /**
     * Select a relation by its property name; waits for the edit form to swap in.
     * A click fires the `mousedown` HTMX trigger the tree node listens for.
     */
    async openRelation(propertyName: string): Promise<void> {
        const node = this.relationsPanel.locator(
            `cds-tree-node[label='${propertyName}']`,
        );
        await expect(node).toBeVisible();
        const load = this.page.waitForResponse(
            (res) =>
                res.url().includes("/relations/edit/") && res.ok(),
        );
        await node.click();
        await load;
        await expect(this.selectedRelation.locator("#relation-edit")).toBeVisible();
    }

    /**
     * Click the relation's Delete button; waits for the confirmation fragment to
     * be fetched into `#modal-container`.
     */
    async clickDelete(): Promise<void> {
        const load = this.page.waitForResponse(
            (res) => res.url().includes("/delete") && res.ok(),
        );
        await this.deleteButton.click();
        await load;
    }

    /** Dismiss the confirmation modal via its Cancel button. */
    async cancelDelete(): Promise<void> {
        await this.cancelButton.click();
    }
}
