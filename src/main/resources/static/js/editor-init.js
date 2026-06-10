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
    // Prevent Ace from injecting inline <style> tags (ace-builds 1.44.0 does not
    // expose a nonce config key). All Ace CSS is served as a static <link> via
    // /assets/css/ace-editor.css, so inline injection is unnecessary.
    // Must be called before any editor is created so the Editor constructor
    // cannot reset it to false.
    ace.config.set("useStrictCSP", true);

    // Execute the language_tools module (the static <script> only registers
    // its ace.define factory) so the autocompletion editor options exist.
    ace.require("ace/ext/language_tools");

    // jq has no Ace mode, so completions come from a static word list
    // (jq 1.6 keywords and builtins).
    var JQ_KEYWORDS = [
        "if", "then", "elif", "else", "end", "as", "def", "reduce",
        "foreach", "try", "catch", "label", "import", "include", "and", "or",
    ];
    var JQ_BUILTINS = [
        "add", "all", "any", "arrays", "ascii_downcase", "ascii_upcase",
        "booleans", "builtins", "capture", "ceil", "combinations", "contains",
        "debug", "del", "delpaths", "empty", "endswith", "env", "error",
        "explode", "fabs", "first", "flatten", "floor", "from_entries",
        "fromdate", "fromdateiso8601", "fromjson", "getpath", "group_by",
        "gsub", "halt", "halt_error", "has", "implode", "in", "index",
        "indices", "infinite", "input", "input_line_number", "inputs",
        "inside", "isinfinite", "isnan", "isnormal", "iterables", "join",
        "keys", "keys_unsorted", "last", "leaf_paths", "length", "limit",
        "ltrimstr", "map", "map_values", "match", "max", "max_by", "min",
        "min_by", "mktime", "nan", "not", "now", "nth", "nulls", "numbers",
        "objects", "path", "paths", "pow", "range", "recurse", "repeat",
        "reverse", "rindex", "round", "rtrimstr", "scalars", "scan",
        "select", "setpath", "sort", "sort_by", "split", "splits", "sqrt",
        "startswith", "stderr", "strings", "strftime", "strptime", "sub",
        "test", "to_entries", "todate", "todateiso8601", "tojson",
        "tonumber", "tostream", "tostring", "transpose", "type", "unique",
        "unique_by", "until", "utf8bytelength", "values", "walk", "while",
        "with_entries",
    ];
    // Context fields IKO exposes to endpoint transforms; the resulting
    // headers are added to the selected connector endpoint request.
    var JQ_IKO_CONTEXT = ["idParam", "sortParams", "filterParams"];
    var jqCompleter = {
        getCompletions: function (editor, session, pos, prefix, callback) {
            callback(
                null,
                JQ_KEYWORDS.map(function (word) {
                    return { caption: word, value: word, meta: "keyword", score: 50 };
                }).concat(
                    JQ_BUILTINS.map(function (word) {
                        return { caption: word, value: word, meta: "builtin", score: 100 };
                    }),
                    JQ_IKO_CONTEXT.map(function (word) {
                        // Caption shows the dotted form; the inserted value has
                        // no dot because Ace's completion prefix stops at "." —
                        // typing ".id" replaces only "id".
                        return {
                            caption: "." + word,
                            value: word,
                            meta: "iko-context",
                            score: 200,
                        };
                    }),
                ),
            );
        },
    };

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
            // htmx's settle phase restores the server-rendered attributes on
            // same-id nodes ~20ms after an outerHTML swap, wiping the classes
            // Ace put on the container (ace_editor, ace-chrome) when the
            // editor was created by an earlier htmx:load/afterSwap. If those
            // classes are gone the editor is visually broken — rebuild it.
            if (el.classList.contains("ace_editor")) {
                return;
            }
            disposeEditor(el);
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

        if (language === "jq" && !isReadOnly) {
            editor.setOptions({
                // Array form replaces the default completers (snippet/keyword
                // ones are useless in the plain-text mode jq runs in).
                enableBasicAutocompletion: [jqCompleter],
                enableLiveAutocompletion: true,
            });
        }

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
        document.querySelectorAll("[data-ace]").forEach(initEditor);
    }

    function disposeEditor(el) {
        if (el && el._editor) {
            if (el._editor._resizeObserver) {
                el._editor._resizeObserver.disconnect();
            }
            el._editor.destroy();
            el._editor = null;
            el.innerHTML = "";
        }
    }

    function disposeEditors(root) {
        root.querySelectorAll("[data-ace]").forEach(disposeEditor);
    }

    // Before a request that will replace an editor's markup, tear the editor
    // down so the swapped-in node re-inits cleanly. The triggering element
    // names the editor via data-editor-id. Needed because morph/reuse swaps can
    // keep the old node (and its _editor), making initEditor early-return.
    document.addEventListener("htmx:beforeRequest", function (e) {
        const editorId = e.detail?.elt?.getAttribute?.("data-editor-id");
        if (editorId) {
            disposeEditor(document.getElementById(editorId));
        }
    });

    // Init on page load
    document.addEventListener("DOMContentLoaded", initAllEditors);

    // Init after HTMX swaps
    document.addEventListener("htmx:afterSwap", initAllEditors);

    // htmx:load fires for every newly inserted node, including server
    // retargeted (HX-Retarget) swaps where htmx:afterSwap can be missed.
    // initEditor guards on el._editor, so this is idempotent.
    document.addEventListener("htmx:load", initAllEditors);

    // Init after HTMX response errors (if swap was skipped)
    document.addEventListener("htmx:responseError", initAllEditors);

    // The settle phase can wipe Ace's container classes (see initEditor);
    // re-check once settling is done.
    document.addEventListener("htmx:afterSettle", initAllEditors);

    // Explicit server-driven re-init: responses that swap in editor markup can
    // send `HX-Trigger: reinitAceEditors`; htmx dispatches it as a DOM event
    // after the swap settles, so editors mount reliably.
    document.addEventListener("reinitAceEditors", initAllEditors);

    // Handle 422 validation errors via htmx:afterRequest
    document.addEventListener("htmx:afterRequest", function (e) {
        const xhr = e.detail.xhr;
        const trigger = e.detail.elt;
        const editorSelector = trigger.getAttribute("data-editor-selector");

        const errorBox = document.getElementById("ace-error");
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
            editor.classList.add("ace-editor-error");
        } else if (xhr.status >= 200 && xhr.status < 300) {
            if (errorBox) {
                errorBox.style.display = "none";
                errorBox.textContent = "";
            }
            editor.classList.remove("ace-editor-error");
        }
    });

    // Dispose editors before HTMX swaps to avoid leaks
    document.addEventListener("htmx:beforeSwap", function (e) {
        const root = e.detail?.target || e.target || document;
        disposeEditors(root);
    });
})();
