const bundles = [
    'ui-shell',
    'accordion', 'breadcrumb', 'button', 'checkbox', 'code-snippet', 'combo-box',
    'content-switcher', 'copy-button', 'data-table', 'date-picker', 'dropdown',
    'file-uploader', 'floating-menu', 'form', 'form-group', 'inline-loading', 'input', 'link',
    'list', 'loading', 'modal', 'multi-select', 'notification', 'number-input',
    'overflow-menu', 'pagination', 'progress-indicator', 'radio-button', 'search',
    'select', 'skeleton-placeholder', 'skeleton-text', 'skip-to-content', 'slider', 'stack',
    'structured-list', 'tabs', 'tag', 'textarea', 'tile', 'toggle', 'tooltip'
];

for (const file of bundles) {
    await import(`https://1.www.s81c.com/common/carbon/web-components/tag/latest/${file}.min.js`);
}