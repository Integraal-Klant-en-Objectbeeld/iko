// Point "vs" to your local path
import * as monaco from "/assets/js/monaco-editor/min/vs/loader.js";

require.config({
    paths: { vs: '/assets/js/monaco-editor/min/vs' }
});

// Simple helper to init Monaco on any [data-monaco] container
(function () {
    const initialized = new WeakMap();

    function initMonaco(el) {
        if (initialized.has(el)) return Promise.resolve(initialized.get(el));

        const language = el.getAttribute('data-language') || 'plaintext';
        const initialValue = el.getAttribute('data-initial') || '';
        const textareaSelector = el.getAttribute('data-textarea'); // CSS selector to sync

        return new Promise((resolve) => {
            require(['vs/editor/editor.main'], function () {
                const editor = monaco.editor.create(el, {
                    value: initialValue,
                    language,
                    theme: 'vs-dark',
                    automaticLayout: true,
                    minimap: { enabled: false }
                });

                initialized.set(el, editor);

                // Sync to hidden <textarea> so HTMX submits current value
                if (textareaSelector) {
                    const ta = document.querySelector(textareaSelector);
                    if (ta) {
                        const push = () => { ta.value = editor.getValue(); };
                        push(); // initial
                        editor.onDidChangeModelContent(push);
                    }
                }

                resolve(editor);
            });
        });
    }

    // Init on page load
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-monaco]').forEach(initMonaco);
    });

    // Init after HTMX swaps (e.g., modal body inserted)
    document.addEventListener('htmx:afterSwap', (e) => {
        const root = e.detail.target || document;
        root.querySelectorAll('[data-monaco]').forEach(initMonaco);
    });

    // Dispose before HTMX replaces DOM to avoid leaks
    document.addEventListener('htmx:beforeSwap', (e) => {
        const root = (e.detail && e.detail.target) || e.target || document;
        root.querySelectorAll('[data-monaco]').forEach((el) => {
            const editor = initialized.get(el);
            if (editor) editor.dispose();
            initialized.delete(el);
        });
    });
})();