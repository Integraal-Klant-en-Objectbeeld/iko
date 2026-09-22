import { expect, test } from "@playwright/test";
import { AdpDetailPage } from "../../pages/AdpDetailPage";

/**
 * Regression coverage for iko-issues#324: the confirmation dialog for deleting an
 * ADP relation never appeared, because the open hook resolved `cds-modal` against
 * the whole document and matched the Preview tab's hidden `#trace-step-modal`
 * instead of the just-swapped relation-delete modal (`#active-modal`). The fix
 * scopes the selector to `#active-modal`.
 *
 * Uses the DRAFT `e2e-rel-fixture` profile + its `e2e-relation` relation from the
 * mounted seed (e2e/seed/V9999.01.01.1__e2e_baseline.sql). The relation must live
 * on a DRAFT profile because the Delete button is disabled on FINAL profiles.
 *
 * The tests only open (and cancel) the confirmation — they never confirm the
 * delete — so the seeded fixture survives reruns against a persistent DB.
 */

const FIXTURE_ID = "e2e0000a-0000-0000-0000-000000000001";
const RELATION_NAME = "e2e-relation";

test.describe("ADP relation delete confirmation", () => {
    test("clicking Delete opens the relation confirmation modal", async ({
        page,
    }) => {
        const detail = new AdpDetailPage(page);
        await detail.gotoRelations(FIXTURE_ID);
        await detail.openRelation(RELATION_NAME);

        await detail.clickDelete();

        await expect(detail.confirmModal).toBeVisible();
        await expect(detail.confirmModal).toContainText(
            "Are you sure you want to delete this relation?",
        );
    });

    test("Cancel closes the confirmation modal", async ({ page }) => {
        const detail = new AdpDetailPage(page);
        await detail.gotoRelations(FIXTURE_ID);
        await detail.openRelation(RELATION_NAME);
        await detail.clickDelete();
        await expect(detail.confirmModal).toBeVisible();

        await detail.cancelDelete();
        await expect(detail.confirmModal).toBeHidden();
    });
});
