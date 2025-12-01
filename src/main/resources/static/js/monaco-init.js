require.config({
    paths: { vs: "https://unpkg.com/monaco-editor@0.52.0/min/vs" }
});

(function () {
    const initialized = new WeakMap();

    function defineThemes() {
        monaco.editor.defineTheme("lightgray-theme", {
            base: "vs",
            inherit: true,
            rules: [],
            colors: {
                "editor.background": "#f5f5f5",
                "editor.lineHighlightBackground": "#eaeaea",
                "editorGutter.background": "#f5f5f5",
            },
        });
    }

    function initMonaco(el) {
        if (initialized.has(el)) return Promise.resolve(initialized.get(el));

        const language = el.getAttribute("data-language") || "plaintext";
        const initialValue = el.getAttribute("data-initial") || "";
        const textAreaSelector = el.getAttribute("data-textarea");
        const isReadOnly = el.hasAttribute("data-readonly");
        const theme = el.getAttribute("data-theme") || "vs";

        return new Promise((resolve) => {
            require(["vs/editor/editor.main"], function () {
                defineThemes();

                const editor = monaco.editor.create(el, {
                    value: initialValue,
                    language,
                    theme,
                    automaticLayout: true,
                    minimap: { enabled: false },
                    readOnly: isReadOnly,
                });

                initialized.set(el, editor);

                if (textAreaSelector) {
                    const textArea = document.querySelector(textAreaSelector);
                    if (textArea) {
                        const push = () => { textArea.value = editor.getValue(); }; // <— fixed
                        push(); // initial sync
                        editor.onDidChangeModelContent(push);
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

    // Init after HTMX swaps
    document.addEventListener("htmx:afterSwap", () => {
        document.querySelectorAll("[data-monaco]").forEach(initMonaco);
    });

    document.addEventListener("htmx:afterRequest", (e) => {
        const xhr = e.detail.xhr;
        const trigger = e.detail.elt;
        const editorSelector = trigger.getAttribute("data-editor-selector");

        const errorBox = document.getElementById("monaco-error");
        const editor= document.querySelector(editorSelector);

        if (!editor) {
            return;
        }

        if (xhr.status === 422 && xhr.getResponseHeader("Content-Type").includes("text/plain")) {
            errorBox.style.display = "block";
            errorBox.textContent = xhr.responseText;
            editor.classList.add("monaco-editor-error");
        } else if (xhr.status >= 200 && xhr.status < 300) {
            if (errorBox) {
                errorBox.style.display = "none";
                editor.classList.remove("monaco-editor-error");
                errorBox.textContent = "";            }
        }
    });

    // Dispose to avoid leaks
    document.addEventListener("htmx:beforeSwap", (e) => {
        const root = e.detail?.target || e.target || document;
        root.querySelectorAll("[data-monaco]").forEach((el) => {
            const editor = initialized.get(el);
            if (editor) editor.dispose();
            initialized.delete(el);
        });
    });
})();