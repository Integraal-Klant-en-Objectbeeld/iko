// Point "vs" to your local path
require.config({
    paths: { vs: "/assets/js/monaco-editor/min/vs" },
});

// Simple helper to init Monaco on any [data-monaco] container
(function () {
    const initialized = new WeakMap();

    function initMonaco(el) {
        if (initialized.has(el)) return Promise.resolve(initialized.get(el));

        const language= el.getAttribute("data-language") || "plaintext";
        const initialValue= el.getAttribute("data-initial") || "";
        const textAreaSelector= el.getAttribute("data-textarea");
        const isReadOnly= el.hasAttribute("data-readonly");

        return new Promise((resolve) => {
            require(["vs/editor/editor.main"], function (index) {
                const editor = monaco.editor.create(el, {
                    value: initialValue,
                    language,
                    theme: "vs",
                    automaticLayout: true,
                    minimap: { enabled: false },
                    readOnly: isReadOnly,
                });

                initialized.set(el, editor);

                // Sync to hidden <textarea>/<input> so HTMX submits current value
                if (textAreaSelector) {
                    const textArea = document.querySelector(textAreaSelector);
                    if (textArea) {
                        const push = () => { textArea.value = editor.getValue(index); };
                        push(); // initial sync
                        if (typeof editor.onDidChangeModelContent === "function") {
                            editor.onDidChangeModelContent(push);
                        } else {
                            // Rare fallback: listen on the model
                            editor.getModel()?.onDidChangeContent?.(push);
                        }
                    } else {
                        console.warn("data-textarea selector did not match any element:", textAreaSelector);
                    }
                }

                resolve(editor);
            });
        });
    }

    // Init on page load
    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll("[data-monaco]").forEach(initMonaco);
    });

    // Init after HTMX swaps (e.g., modal body inserted)
    document.addEventListener("htmx:afterSwap", (e) => {
        const root = e.detail?.target || document;
        root.querySelectorAll("[data-monaco]").forEach(initMonaco);
    });

    // Dispose before HTMX replaces DOM to avoid leaks
    document.addEventListener("htmx:beforeSwap", (e) => {
        const root = e.detail?.target || e.target || document;
        root.querySelectorAll("[data-monaco]").forEach((el) => {
            const editor = initialized.get(el);
            if (editor) editor.dispose();
            initialized.delete(el);
        });
    });

    // FINAL SAFETY: force-sync just before any HTMX request goes out
    document.addEventListener("htmx:configRequest", () => {
        document.querySelectorAll("[data-monaco][data-textarea]").forEach((el) => {
            const editor = initialized.get(el);
            const taSel = el.getAttribute("data-textarea");
            const ta = taSel ? document.querySelector(taSel) : null;
            if (editor && ta) ta.value = editor.getValue();
        });
    });
})();