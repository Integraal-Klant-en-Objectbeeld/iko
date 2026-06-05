/*
 Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.

 Licensed under EUPL, Version 1.2 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

(function () {
    // Strict-CSP config: no eval, no blob worker
    ace.config.set("basePath", "/assets/js/ace");
    ace.config.set("loadWorkerFromBlob", false);
    // Nonce for Ace's injected <style> tags. Not all ace-builds versions
    // expose this config key; guard so an unknown-key error cannot abort init.
    // style-src keeps 'unsafe-inline' until Phase 5, which switches to a static
    // theme <link> if no usable nonce hook exists.
    try {
        ace.config.set(
            "nonce",
            document.querySelector("meta[name='csp-nonce']")?.content,
        );
    } catch (e) {
        /* ace version lacks nonce config key */
    }

    function mapLanguage(lang) {
        if (lang === "json") return "json";
        if (lang === "yaml") return "yaml";
        // jq has no Ace mode — use plain text
        return "text";
    }

    function mapTheme(theme) {
        // Map legacy theme names to Ace equivalents
        if (theme === "lightgray-theme" || theme === "vs") return "ace/theme/chrome";
        return "ace/theme/chrome";
    }

    function initEditor(el) {
        if (el._editor) {
            return;
        }

        const language = el.getAttribute("data-language") || "text";
        const textAreaSelector = el.getAttribute("data-textarea");
        const isReadOnly =
            el.hasAttribute("data-readonly") &&
            el.getAttribute("data-readonly") !== "false";
        const theme = el.getAttribute("data-theme") || "vs";
        const useWorker = language === "json";

        let initialValue = el.getAttribute("data-initial") || "";

        if (el.hasAttribute("data-format-json")) {
            try {
                initialValue = JSON.stringify(JSON.parse(initialValue), null, 2);
            } catch (e) {
                // keep as-is
            }
        }

        // Clear element content before mounting
        el.innerHTML = "";

        const editor = ace.edit(el, {
            readOnly: isReadOnly,
            useWorker: useWorker,
            theme: mapTheme(theme),
            mode: "ace/mode/" + mapLanguage(language),
            value: initialValue,
            showPrintMargin: false,
        });

        // Position cursor at start rather than end
        editor.clearSelection();
        editor.moveCursorTo(0, 0);

        el._editor = editor;

        // Paint at the container's final size immediately so the editor does
        // not visibly snap when content/data lands later.
        editor.resize(true);

        // The editor may be mounted inside a hidden Carbon tab panel
        // (display:none), so Ace measures a 0px container. Re-measure only when
        // the container's size actually changes (e.g. its tab becomes visible);
        // an unchanged size makes resize() a no-op, so no visible jump.
        if (typeof ResizeObserver !== "undefined") {
            const observer = new ResizeObserver(function () {
                editor.resize();
            });
            observer.observe(el);
            editor._resizeObserver = observer;
        }

        if (textAreaSelector) {
            const textArea = document.querySelector(textAreaSelector);
            if (textArea) {
                // Initial sync
                textArea.value = editor.getValue();
                editor.session.on("change", function () {
                    textArea.value = editor.getValue();
                });
            } else {
                console.warn(
                    "data-textarea selector did not match any element:",
                    textAreaSelector,
                );
            }
        }
    }

    function initAllEditors() {
        document.querySelectorAll("[data-monaco]").forEach(initEditor);
    }

    function disposeEditors(root) {
        root.querySelectorAll("[data-monaco]").forEach(function (el) {
            if (el._editor) {
                if (el._editor._resizeObserver) {
                    el._editor._resizeObserver.disconnect();
                }
                el._editor.destroy();
                el._editor = null;
                el.innerHTML = "";
            }
        });
    }

    // Init on page load
    document.addEventListener("DOMContentLoaded", initAllEditors);

    // Init after HTMX swaps
    document.addEventListener("htmx:afterSwap", initAllEditors);

    // Init after HTMX response errors (if swap was skipped)
    document.addEventListener("htmx:responseError", initAllEditors);

    // Handle 422 validation errors via htmx:afterRequest
    document.addEventListener("htmx:afterRequest", function (e) {
        const xhr = e.detail.xhr;
        const trigger = e.detail.elt;
        const editorSelector = trigger.getAttribute("data-editor-selector");

        const errorBox = document.getElementById("monaco-error");
        const editor = editorSelector
            ? document.querySelector(editorSelector)
            : null;

        if (!editor) {
            return;
        }

        if (
            xhr.status === 422 &&
            xhr.getResponseHeader("Content-Type") &&
            xhr.getResponseHeader("Content-Type").includes("text/plain")
        ) {
            if (errorBox) {
                errorBox.style.display = "block";
                errorBox.textContent = xhr.responseText;
            }
            editor.classList.add("monaco-editor-error");
        } else if (xhr.status >= 200 && xhr.status < 300) {
            if (errorBox) {
                errorBox.style.display = "none";
                errorBox.textContent = "";
            }
            editor.classList.remove("monaco-editor-error");
        }
    });

    // Dispose editors before HTMX swaps to avoid leaks
    document.addEventListener("htmx:beforeSwap", function (e) {
        const root = e.detail?.target || e.target || document;
        disposeEditors(root);
    });
})();
