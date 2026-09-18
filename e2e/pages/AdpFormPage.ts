import { expect, Locator, Page } from "@playwright/test";

/**
 * Page Object for the Aggregated Data Profile create modal (`add.html`, swapped
 * into `#modal-container` as `#active-modal`) and the inline edit form on the
 * detail page's General tab (`edit-panel.html`, `#profile-edit-form`).
 *
 * Field handling:
 *  - `name` / `roles` are Carbon `cds-text-input` hosts; Playwright auto-pierces
 *    their Shadow DOM, so we `.fill()` the inner input.
 *  - `connectorInstanceId` / `connectorEndpointId` are `cds-select` hosts. The Add
 *    form pre-selects the first option (the single seeded e2e instance/endpoint),
 *    so by default no interaction is needed; `selectInstance`/`selectEndpoint`
 *    drive them via the `.value` property + `cds-select-selected` event when an
 *    explicit choice is required.
 *  - `endpointTransform` / `resultTransform` are Ace editors backed by light-DOM
 *    hidden `<textarea>`s (the Add form ships valid defaults `{}` and `.`). We
 *    drive the Ace instance (`el._editor.setValue`) so the change listener syncs
 *    the textarea the form submits.
 *
 * Create posts the modal form; the server replies with `HX-Trigger: close-modal`
 * and pushes the new profile's detail URL, so `submitCreate()` waits for the modal
 * to hide and the URL to land on the detail page. Edit submits the inline form and
 * the server re-renders `#view-panel`, so `submitEdit()` waits for the PUT to
 * complete and the detail page to re-render.
 */
export class AdpFormPage {
    readonly modal: Locator;
    readonly name: Locator;
    readonly instanceSelect: Locator;
    readonly endpointSelect: Locator;
    readonly roles: Locator;
    readonly saveButton: Locator;

    constructor(private readonly page: Page) {
        this.modal = page.locator("#active-modal");
        this.name = this.modal.locator("cds-text-input[name='name'] input");
        this.instanceSelect = this.modal.locator(
            "cds-select[name='connectorInstanceId']",
        );
        this.endpointSelect = this.modal.locator(
            "cds-select[name='connectorEndpointId']",
        );
        this.roles = this.modal.locator("cds-text-input[name='roles'] input");
        this.saveButton = this.modal.locator(
            "cds-modal-footer-button[type='submit']",
        );
    }

    /**
     * Fill the ADP create modal. The connector instance/endpoint selects default
     * to the single seeded e2e instance/endpoint, so only name + roles are
     * required; pass `connectorInstanceId`/`connectorEndpointId` to override.
     */
    async fillAdd(values: {
        name: string;
        roles?: string;
        connectorInstanceId?: string;
        connectorEndpointId?: string;
        endpointTransform?: string;
        resultTransform?: string;
    }): Promise<void> {
        await this.name.fill(values.name);
        await this.roles.fill(values.roles ?? "ROLE_ADMIN");
        if (values.connectorInstanceId !== undefined) {
            await this.selectInstance(values.connectorInstanceId);
        }
        if (values.connectorEndpointId !== undefined) {
            await this.selectEndpoint(values.connectorEndpointId);
        }
        if (values.endpointTransform !== undefined) {
            await this.setAceValue(
                "#endpointTransform-editor-add",
                "#endpointTransformCodeAdd",
                values.endpointTransform,
            );
        }
        if (values.resultTransform !== undefined) {
            await this.setAceValue(
                "#result-transform-editor-add",
                "#resultTransformCodeAdd",
                values.resultTransform,
            );
        }
    }

    /**
     * Set the connector-instance select and fire the change event the form's
     * endpoint-reload HTMX trigger listens for.
     */
    async selectInstance(connectorInstanceId: string): Promise<void> {
        await this.instanceSelect.evaluate((el, value) => {
            (el as unknown as { value: string }).value = value;
            el.dispatchEvent(
                new CustomEvent("cds-select-selected", { bubbles: true }),
            );
        }, connectorInstanceId);
    }

    /** Set the connector-endpoint select value. */
    async selectEndpoint(connectorEndpointId: string): Promise<void> {
        await this.endpointSelect.evaluate((el, value) => {
            (el as unknown as { value: string }).value = value;
            el.dispatchEvent(
                new CustomEvent("cds-select-selected", { bubbles: true }),
            );
        }, connectorEndpointId);
    }

    /** Submit the create modal and wait for it to close and the detail page to load. */
    async submitCreate(): Promise<void> {
        await this.saveButton.click();
        // close-modal removes the modal's `open` attribute; create also pushes the
        // new profile's detail URL into the address bar.
        await this.page.waitForURL(
            /\/admin\/aggregated-data-profiles\/[0-9a-f-]+/,
        );
        await expect(this.modal).toBeHidden();
    }

    /**
     * Drive an Ace editor (light-DOM textarea sync) so the submitted form carries
     * the value.
     */
    private async setAceValue(
        editorSelector: string,
        textareaSelector: string,
        value: string,
    ): Promise<void> {
        const editor = this.page.locator(editorSelector);
        await expect(editor).toHaveClass(/ace_editor/);
        await editor.evaluate((el, v) => {
            const ace = (
                el as unknown as {
                    _editor?: { setValue: (val: string, cursor?: number) => void };
                }
            )._editor;
            if (!ace) {
                throw new Error(`Ace editor not initialised on ${el.id}`);
            }
            ace.setValue(v, -1);
        }, value);
        await expect(this.page.locator(textareaSelector)).toHaveValue(value);
    }
}

/**
 * Page Object for the inline ADP edit form on the detail page General tab.
 *
 * The form (`#profile-edit-form`) lives inside `#profile-edit`; its only freely
 * editable field on a DRAFT profile is `roles` (name is immutable, the
 * instance/endpoint selects are populated from the existing profile). Submitting
 * via `#profile-save-btn` issues a PUT; the server re-renders `#view-panel`, so we
 * wait for the PUT response and the re-rendered detail page.
 */
export class AdpEditForm {
    readonly form: Locator;
    readonly roles: Locator;
    readonly saveButton: Locator;

    constructor(private readonly page: Page) {
        this.form = page.locator("#profile-edit-form");
        this.roles = this.form.locator("cds-text-input[name='roles'] input");
        this.saveButton = page.locator("#profile-save-btn");
    }

    /** Replace the roles value on the edit form. */
    async setRoles(roles: string): Promise<void> {
        await expect(this.roles).toBeVisible();
        await this.roles.fill(roles);
    }

    /** Submit the edit form and wait for the PUT + detail-page re-render. */
    async submit(): Promise<void> {
        const put = this.page.waitForResponse(
            (res) =>
                res.request().method() === "PUT" &&
                res.url().includes("/admin/aggregated-data-profiles") &&
                res.ok(),
        );
        await this.saveButton.click();
        await put;
        // The re-rendered detail page re-mounts the General tab edit form.
        await expect(this.page.locator("#profile-edit-form")).toBeVisible();
    }
}
