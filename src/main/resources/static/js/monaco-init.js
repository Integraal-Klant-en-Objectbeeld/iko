require.config({
    paths: { vs: "https://unpkg.com/monaco-editor@0.52.0/min/vs" }
});

(function () {
    const initialized = new WeakMap();
    const JQ_LANGUAGE_DEFINITION_URL = "/assets/js/monaco-jq.js";
    let jqLanguagePromise;
    let jqLanguageRegistered = false;

    function evaluateCommonJsModule(source) {
        const module = { exports: {} };
        Object.defineProperty(module, "export", {
            enumerable: true,
            configurable: true,
            get() {
                return module.exports;
            },
            set(value) {
                module.exports = value;
            },
        });

        const wrapper = new Function("module", "exports", source);
        wrapper(module, module.exports);

        return module.exports?.default ?? module.exports;
    }

    function loadJqLanguageDefinition() {
        return fetch(JQ_LANGUAGE_DEFINITION_URL, { credentials: "omit" })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`Failed to load jq language definition: ${response.status} ${response.statusText}`);
                }
                return response.text();
            })
            .then(evaluateCommonJsModule);
    }

    function configureJqLanguage(languageDefinitionModule) {
        const definition = languageDefinitionModule?.default ?? languageDefinitionModule;
        const languageDefinition = definition?.language ?? definition;

        if (!languageDefinition || typeof languageDefinition !== "object") {
            throw new Error("Invalid jq language definition");
        }

        if (!jqLanguageRegistered) {
            monaco.languages.register({ id: "jq" });
            jqLanguageRegistered = true;
        }

        monaco.languages.setMonarchTokensProvider("jq", languageDefinition);

        const configuration = definition?.conf ?? definition?.configuration;
        if (configuration) {
            monaco.languages.setLanguageConfiguration("jq", configuration);
        }
    }

    function ensureLanguageSupport(languageId) {
        if (languageId !== "jq") {
            return Promise.resolve();
        }
        if (jqLanguagePromise) {
            return jqLanguagePromise;
        }

        jqLanguagePromise = loadJqLanguageDefinition()
            .then((languageDefinitionModule) => configureJqLanguage(languageDefinitionModule))
            .catch((error) => {
                jqLanguagePromise = undefined;
                throw error;
            });

        return jqLanguagePromise;
    }

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
        const existing = initialized.get(el);
        if (existing?.editor) {
            return Promise.resolve(existing.editor);
        }
        if (existing?.promise) {
            return existing.promise;
        }

        const state = {
            editor: null,
            resizeObserver: null,
            promise: null
        };
        initialized.set(el, state);

        const language = el.getAttribute("data-language") || "plaintext";
        const initialValue = el.getAttribute("data-initial") || "";
        const textAreaSelector = el.getAttribute("data-textarea");
        const isReadOnly = el.hasAttribute("data-readonly");
        const theme = el.getAttribute("data-theme") || "vs";

        const initPromise = new Promise((resolve) => {
            require(["vs/editor/editor.main"], function () {
                defineThemes();

                const startEditor = () => {
                    // Clear any existing content in the element
                    el.innerHTML = '';

                    const editor = monaco.editor.create(el, {
                        value: initialValue,
                        language,
                        theme,
                        automaticLayout: false,
                        minimap: { enabled: false },
                        readOnly: isReadOnly,
                        scrollBeyondLastLine: false,
                    });

                    el._monacoEditor = editor; // Store reference on element for external access

                    // Sync editor content to textarea
                    if (textAreaSelector) {
                        const textArea = document.querySelector(textAreaSelector);
                        if (textArea) {
                            const push = () => { textArea.value = editor.getValue(); };
                            push(); // initial sync
                            editor.onDidChangeModelContent(push);
                        } else {
                            console.warn("data-textarea selector did not match any element:", textAreaSelector);
                        }
                    }

                    // Use ResizeObserver to watch for container size changes
                    const resizeObserver = new ResizeObserver(() => {
                        editor.layout();
                    });
                    resizeObserver.observe(el);

                    // Store both editor and observer for cleanup
                    state.editor = editor;
                    state.resizeObserver = resizeObserver;

                    // Initial layout
                    requestAnimationFrame(() => editor.layout());

                    resolve(editor);
                };

                ensureLanguageSupport(language)
                    .catch((error) => {
                        console.error("Failed to load the Monaco jq language definition:", error);
                    })
                    .then(startEditor);
            });
        });

        state.promise = initPromise;
        return initPromise;
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
            const state = initialized.get(el);
            if (state) {
                // Dispose editor if created
                state.editor?.dispose();
                // Disconnect resize observer to prevent memory leaks
                state.resizeObserver?.disconnect();
            }
            initialized.delete(el);
            // Clear the element to prevent layout issues
            el.innerHTML = '';
        });
    });
})();
