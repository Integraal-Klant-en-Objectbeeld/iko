import { test as setup } from "@playwright/test";
import { LoginPage } from "../pages/LoginPage";
import { globalLoginAndSaveState } from "../fixtures/auth";

/**
 * `setup` project entry: logs in as admin against live Keycloak and saves the
 * authenticated storageState to `.auth/admin.json`. Dependent projects load it
 * via `storageState` so they start already authenticated.
 */
setup("authenticate as admin", async ({ page }) => {
    const loginPage = new LoginPage(page);
    await globalLoginAndSaveState(loginPage, (path) =>
        page.context().storageState({ path }),
    );
});
