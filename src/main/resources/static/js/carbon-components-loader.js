// /assets/js/carbon-ibm-loader.js
const VERSION = window.CARBON_CDN_VERSION || 'v2.40.1';
const BASE = `https://1.www.s81c.com/common/carbon/web-components/version/${VERSION}`;

const COMPONENTS = [
    "ui-shell",
    "accordion",
    "breadcrumb",
    "button",
    "checkbox",
    "code-snippet",
    "combo-box",
    "content-switcher",
    "copy-button",
    "data-table",
    "date-picker",
    "dropdown",
    "file-uploader",
    "form",
    "inline-loading",
    "text-input",
    "link",
    "loading",
    "modal",
    "multi-select",
    "notification",
    "number-input",
    "overflow-menu",
    "pagination",
    "progress-indicator",
    "radio-button",
    "search",
    "select",
    "skeleton-placeholder",
    "skeleton-text",
    "skip-to-content",
    "slider",
    "structured-list",
    "tabs",
    "tag",
    "textarea",
    "tile",
    "toggle",
    "tooltip",
];

if (!window.__CARBON_IBM_LOADER__) {
    window.__CARBON_IBM_LOADER__ = (async () => {
        const started = performance.now();
        for (const name of COMPONENTS) {
            const url = `${BASE}/${name}.min.js`;
            try {
                await import(url);
            } catch (err) {
                console.error(`[Carbon IBM Loader] Failed to load ${url}`, err);
            }
        }
        const ms = Math.round(performance.now() - started);
    })();
}