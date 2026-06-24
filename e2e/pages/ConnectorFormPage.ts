import { expect, Locator, Page } from "@playwright/test";

/**
 * Page Object for the Connector create modal and the Connector Instance create
 * modal (both swapped into `#modal-container` as `#active-modal`).
 *
 * Field handling:
 *  - `name` / `reference` / `apiSpecificationUrl` are Carbon `cds-text-input`
 *    hosts; Playwright auto-pierces their Shadow DOM, so we `.fill()` the host.
 *  - `connectorCode` is an Ace editor (`#connectorCodeEditor`) that syncs into a
 *    light-DOM hidden `<textarea id="connectorCodeField" name="connectorCode">`.
 *    Ace overwrites the textarea on every `change`, so we drive the Ace instance
 *    (`el._editor.setValue`) rather than writing the textarea directly — that
 *    fires the change listener and leaves the textarea holding our value.
 *
 * Submit posts the modal form and the server replies with `HX-Trigger: close-modal`.
 * admin-ui.js handles that event by removing the modal's `open` attribute (it does
 * not delete the element), so `submit()` waits for the modal to become hidden.
 */
export class ConnectorFormPage {
    readonly modal: Locator;
    readonly name: Locator;
    readonly reference: Locator;
    readonly apiSpecificationUrl: Locator;
    readonly saveButton: Locator;

    constructor(private readonly page: Page) {
        this.modal = page.locator("#active-modal");
        // Carbon `cds-text-input` renders its real <input> in Shadow DOM; the host
        // carries the stable name=. Target the inner input so .fill() works.
        this.name = this.modal.locator("cds-text-input[name='name'] input");
        this.reference = this.modal.locator(
            "cds-text-input[name='reference'] input",
        );
        this.apiSpecificationUrl = this.modal.locator(
            "cds-text-input[name='apiSpecificationUrl'] input",
        );
        this.saveButton = this.modal.locator(
            "cds-modal-footer-button[type='submit']",
        );
    }

    /** Fill the connector create modal (name, reference, valid connector code). */
    async fillConnector(values: {
        name: string;
        reference: string;
        connectorCode?: string;
    }): Promise<void> {
        await this.name.fill(values.name);
        await this.reference.fill(values.reference);
        const code = values.connectorCode ?? defaultConnectorCode(values.reference);
        await this.setConnectorCode(code);
    }

    /** Fill the connector instance create modal (name, reference, optional URL). */
    async fillInstance(values: {
        name: string;
        reference: string;
        apiSpecificationUrl?: string;
    }): Promise<void> {
        await this.name.fill(values.name);
        await this.reference.fill(values.reference);
        if (values.apiSpecificationUrl !== undefined) {
            await this.apiSpecificationUrl.fill(values.apiSpecificationUrl);
        }
    }

    /**
     * Write into the connector-code Ace editor and let it sync the hidden
     * textarea that the form submits.
     */
    async setConnectorCode(code: string): Promise<void> {
        const editor = this.page.locator("#connectorCodeEditor");
        await expect(editor).toHaveClass(/ace_editor/);
        await editor.evaluate((el, value) => {
            const ace = (el as unknown as { _editor?: { setValue: (v: string, c?: number) => void } })
                ._editor;
            if (!ace) {
                throw new Error("Ace editor not initialised on #connectorCodeEditor");
            }
            ace.setValue(value, -1);
        }, code);
        // Confirm the sync reached the submitted textarea.
        await expect(this.page.locator("#connectorCodeField")).toHaveValue(code);
    }

    /** Submit the modal form and wait for the close-modal trigger to hide it. */
    async submit(): Promise<void> {
        await this.saveButton.click();
        // close-modal removes the `open` attribute; the modal becomes hidden.
        await expect(this.modal).toBeHidden();
    }
}

/**
 * Minimal valid connector code whose single route URI matches `reference` — the
 * server validates that `direct:iko:connector:{tag}` equals the connector tag
 * before persisting.
 */
export function defaultConnectorCode(reference: string): string {
    return [
        "- route:",
        `    id: "direct:iko:connector:${reference}"`,
        "    errorHandler:",
        "        noErrorHandler: { }",
        "    from:",
        `        uri: "direct:iko:connector:${reference}"`,
        "        steps:",
        '            - log: "e2e created connector"',
    ].join("\n");
}
