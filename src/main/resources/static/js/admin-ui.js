/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Disable HTMX eval-based features (hx-on, hx-vals="js:", js: trigger filters).
htmx.config.allowEval = false;
htmx.config.allowNestedOobSwaps = false;

// ---------------------------------------------------------------------------
// Utility helpers
// ---------------------------------------------------------------------------
function getCookieValue(name) {
    const match = document.cookie.match(
        new RegExp(
            "(?:^|; )" + name.replace(/[.$?*|{}()[\]\\/+^]/g, "\\$&") + "=([^;]*)"
        )
    );
    return match ? decodeURIComponent(match[1]) : null;
}

// ---------------------------------------------------------------------------
// CSRF — attach X-XSRF-TOKEN to every mutating HTMX request
// ---------------------------------------------------------------------------
document.body.addEventListener("htmx:configRequest", function (event) {
    const method = (event.detail.verb || "get").toLowerCase();
    if (method === "get" || method === "head" || method === "options") {
        return;
    }
    const token = getCookieValue("XSRF-TOKEN");
    if (token) {
        event.detail.headers["X-XSRF-TOKEN"] = token;
    }
});

// ---------------------------------------------------------------------------
// Close-modal helper (dispatched by server OOB responses)
// ---------------------------------------------------------------------------
document.body.addEventListener("close-modal", function () {
    document
        .querySelector("#modal-container #active-modal")
        ?.removeAttribute("open");
});

// ---------------------------------------------------------------------------
// Logout — triggered by id="logout-link"
// ---------------------------------------------------------------------------
document.body.addEventListener("click", function (event) {
    const link = event.target.closest("#logout-link");
    if (!link) return;
    event.preventDefault();
    const form = document.createElement("form");
    form.method = "POST";
    form.action = "/logout";
    const token = getCookieValue("XSRF-TOKEN");
    if (token) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = "_csrf";
        input.value = token;
        form.appendChild(input);
    }
    document.body.appendChild(form);
    form.submit();
});

// ---------------------------------------------------------------------------
// Side-nav active highlight (replaces hx-on::after-request on each nav link)
// ---------------------------------------------------------------------------
document.body.addEventListener("htmx:afterRequest", function (event) {
    const link = event.target.closest("cds-side-nav-link");
    if (!link) return;
    document
        .querySelectorAll("cds-side-nav-link")
        .forEach(function (el) {
            el.removeAttribute("active");
        });
    link.toggleAttribute("active");
});

// ---------------------------------------------------------------------------
// hx-vals replacement: inject dynamic query parameters into HTMX requests.
//
// Elements that previously used hx-vals="js:{...}" now carry a
// data-hx-dynamic-params attribute whose value is a comma-separated list of
// parameter names to resolve.  The resolver map below knows how to obtain
// each parameter value from the DOM at request time.
// ---------------------------------------------------------------------------
var _paramResolvers = {
    // Pagination / filter shared params
    paginationPage: function () {
        var el = document.querySelector("#pagination-container");
        return el ? String((el.page || 1) - 1) : "0";
    },
    paginationSize: function () {
        var el = document.querySelector("#pagination-container");
        return el ? String(el.pageSize || 10) : "10";
    },
    searchBarQuery: function () {
        var el = document.querySelector("#search-bar");
        return el ? el.value || "" : "";
    },
    activeFilterToggle: function () {
        var el = document.querySelector("#active-filter-toggle");
        return el ? String(el.checked !== false) : "true";
    },
    urlSort: function () {
        return new URLSearchParams(window.location.search).get("sort") || "";
    },
    // Sort from event detail (cds-table-header-cell-sort)
    headerCellSort: function (event) {
        var dir = event && event.detail && event.detail.sortDirection;
        var map = { none: "", ascending: "ASC", descending: "DESC" };
        return "name," + (map[dir] || "");
    },
    // Logging pagination
    logPaginationPage: function () {
        var el = document.querySelector("#log-pagination-container");
        return el ? String((el.page || 1) - 1) : "0";
    },
    logPaginationSize: function () {
        var el = document.querySelector("#log-pagination-container");
        return el ? String(el.pageSize || 10) : "10";
    },
};

document.body.addEventListener("htmx:configRequest", function (event) {
    var el = event.detail.elt;
    if (!el || !el.dataset) return;
    var params = el.dataset.hxDynamicParams;
    if (!params) return;

    // Trigger event (the native DOM event that triggered HTMX)
    var triggerEvent = event.detail.triggeringEvent;

    params.split(",").forEach(function (entry) {
        entry = entry.trim();
        // Each entry is "paramName:resolverKey"
        var parts = entry.split(":");
        var paramName = parts[0].trim();
        var resolverKey = parts[1] ? parts[1].trim() : paramName;
        var resolver = _paramResolvers[resolverKey];
        if (resolver) {
            event.detail.parameters[paramName] = resolver(triggerEvent);
        }
    });
});

// ---------------------------------------------------------------------------
// "active-filter-toggle" checked value for checkbox-changed events:
// The event detail has .checked, so we need a special resolver.
// ---------------------------------------------------------------------------
document.body.addEventListener("htmx:configRequest", function (event) {
    var el = event.detail.elt;
    if (!el || !el.dataset) return;
    var params = el.dataset.hxDynamicParams;
    if (!params) return;
    // isActive from event.detail.checked (cds-checkbox-changed)
    if (params.includes("isActive:eventChecked")) {
        var triggerEvent = event.detail.triggeringEvent;
        if (triggerEvent && triggerEvent.detail && "checked" in triggerEvent.detail) {
            event.detail.parameters["isActive"] = String(triggerEvent.detail.checked);
        }
    }
    // sort from event.detail.sortDirection (cds-table-header-cell-sort)
    if (params.includes("sort:headerCellSortName")) {
        var triggerEvent2 = event.detail.triggeringEvent;
        var dir = triggerEvent2 && triggerEvent2.detail && triggerEvent2.detail.sortDirection;
        var map = { none: "", ascending: "ASC", descending: "DESC" };
        event.detail.parameters["sort"] = "name," + (map[dir] || "");
    }
    if (params.includes("sort:headerCellSortTimestamp")) {
        var triggerEvent3 = event.detail.triggeringEvent;
        var dir3 = triggerEvent3 && triggerEvent3.detail && triggerEvent3.detail.sortDirection;
        var map3 = { none: "", ascending: "ASC", descending: "DESC" };
        event.detail.parameters["sort"] = "timestamp," + (map3[dir3] || "");
    }
});

// ---------------------------------------------------------------------------
// "Get results" button (preview-panel): before-request label toggle
// ---------------------------------------------------------------------------
document.body.addEventListener("htmx:beforeRequest", function (event) {
    var el = event.detail.elt;
    if (!el) return;
    if (el.dataset && el.dataset.beforeRequestLabel === "testing") {
        el.dataset.label = el.textContent;
        el.textContent = "Testing…";
        el.setAttribute("disabled", "");
    }
});

document.body.addEventListener("htmx:afterRequest", function (event) {
    var el = event.detail.elt;
    if (!el) return;
    if (el.dataset && el.dataset.beforeRequestLabel === "testing") {
        if (el.dataset.label) {
            el.textContent = el.dataset.label;
        }
        el.removeAttribute("disabled");
    }
});

// ---------------------------------------------------------------------------
// Schema regenerate button: before/after request loading state
// ---------------------------------------------------------------------------
document.body.addEventListener("htmx:beforeRequest", function (event) {
    var el = event.detail.elt;
    if (!el) return;
    if (el.dataset && el.dataset.loadingText) {
        el.dataset.originalText = el.textContent.trim();
        el.setAttribute("disabled", "");
        el.textContent = el.dataset.loadingText;
    }
});

document.body.addEventListener("htmx:afterRequest", function (event) {
    var el = event.detail.elt;
    if (!el) return;
    if (el.dataset && el.dataset.loadingText && el.dataset.originalText) {
        el.textContent = el.dataset.originalText;
        el.removeAttribute("disabled");
    }
});

// ---------------------------------------------------------------------------
// Save button loading state (profile-edit-form, instance-save-btn)
// Elements with data-save-btn-id="someId" disable that button on before-request
// and restore it on after-request.
// ---------------------------------------------------------------------------
document.body.addEventListener("htmx:beforeRequest", function (event) {
    var el = event.detail.elt;
    if (!el || !el.dataset || !el.dataset.saveBtnId) return;
    var btn = document.getElementById(el.dataset.saveBtnId);
    if (btn) {
        btn.setAttribute("disabled", "");
        btn.textContent = el.dataset.savingText || "Saving…";
    }
});

document.body.addEventListener("htmx:afterRequest", function (event) {
    var el = event.detail.elt;
    if (!el || !el.dataset || !el.dataset.saveBtnId) return;
    var btn = document.getElementById(el.dataset.saveBtnId);
    if (btn) {
        btn.removeAttribute("disabled");
        btn.textContent = el.dataset.savedText || "Save";
    }
});

// ---------------------------------------------------------------------------
// hx-before-swap: allow error responses to swap into the target
// Elements with data-swap-errors="true" set isError=false so HTMX swaps even
// on 4xx/5xx responses.
// ---------------------------------------------------------------------------
document.body.addEventListener("htmx:beforeSwap", function (event) {
    var el = event.detail.elt;
    if (!el || !el.dataset) return;
    if (el.dataset.swapErrors === "true" && event.detail.isError) {
        event.detail.shouldSwap = true;
        event.detail.isError = false;
    }
});

// ---------------------------------------------------------------------------
// hx-on:click="event.stopPropagation()" replacement:
// overflow-menu elements with data-stop-propagation="true".
// Must be a bubble listener on the element itself: a capture listener on
// document.body stops the click before it reaches the menu trigger inside
// the component's shadow DOM, so the overflow menu never opens. A bubble
// listener here still prevents the click from reaching the clickable table
// row (htmx hx-get) above it.
// ---------------------------------------------------------------------------
function bindStopPropagation() {
    document
        .querySelectorAll("[data-stop-propagation]")
        .forEach(function (el) {
            if (el._stopPropagationBound) return;
            el._stopPropagationBound = true;
            el.addEventListener("click", function (event) {
                event.stopPropagation();
            });
        });
}
document.addEventListener("DOMContentLoaded", bindStopPropagation);
document.body.addEventListener("htmx:load", bindStopPropagation);

// ---------------------------------------------------------------------------
// relation/delete.html: cancel button opens/closes the modal
// Elements with data-toggle-modal-selector="<selector>"
// ---------------------------------------------------------------------------
document.body.addEventListener("click", function (event) {
    var btn = event.target.closest("[data-toggle-modal-selector]");
    if (!btn) return;
    var sel = btn.dataset.toggleModalSelector;
    var modal = document.querySelector(sel);
    if (modal) {
        modal.toggleAttribute("open");
    }
});

// ---------------------------------------------------------------------------
// relation/list.html: after-request open modal
// Elements with data-after-request-open-modal on the HTMX element.
// Uses htmx:afterRequest already handled below via selector.
// ---------------------------------------------------------------------------
document.body.addEventListener("htmx:afterRequest", function (event) {
    var el = event.detail.elt;
    if (!el || !el.dataset) return;
    if (el.dataset.afterRequestOpenModal) {
        var modal = document.querySelector(el.dataset.afterRequestOpenModal);
        if (modal) {
            modal.setAttribute("open", "true");
        }
    }
});

// ---------------------------------------------------------------------------
// finalize modals: after-request close active-modal
// Elements with data-after-request-close-modal="true"
// ---------------------------------------------------------------------------
document.body.addEventListener("htmx:afterRequest", function (event) {
    var el = event.detail.elt;
    if (!el || !el.dataset) return;
    if (el.dataset.afterRequestCloseModal === "true") {
        document.getElementById("active-modal")?.removeAttribute("open");
    }
});

// ---------------------------------------------------------------------------
// logging/detail-modal.html close button
// Elements with data-close-logging-modal="true"
// ---------------------------------------------------------------------------
document.body.addEventListener("click", function (event) {
    var btn = event.target.closest("[data-close-logging-modal]");
    if (!btn) return;
    var modal = document.getElementById("logging-detail-modal");
    if (modal) {
        modal.open = false;
    }
    var container = document.getElementById("modal-container");
    if (container) {
        container.innerHTML = "";
    }
});

// ---------------------------------------------------------------------------
// edit-panel.html: save button click submits the form
// Elements with data-submit-form-id="<formId>"
// ---------------------------------------------------------------------------
document.body.addEventListener("click", function (event) {
    var btn = event.target.closest("[data-submit-form-id]");
    if (!btn) return;
    var form = document.getElementById(btn.dataset.submitFormId);
    if (form) {
        form.requestSubmit();
    }
});

// ---------------------------------------------------------------------------
// relations-panel.html: mousedown on tree nodes sets selectedSourceId hidden
// Elements with data-set-source-id="<value>" or data-set-source-id-from-attr="<attrName>"
// ---------------------------------------------------------------------------
document.body.addEventListener("mousedown", function (event) {
    var node = event.target.closest("[data-set-source-id]");
    if (node) {
        var hiddenInput = document.querySelector("#selectedSourceId");
        if (hiddenInput) {
            hiddenInput.value = node.dataset.setSourceId;
        }
        return;
    }
    var nodeAttr = event.target.closest("[data-set-source-id-from-attr]");
    if (nodeAttr) {
        var attrName = nodeAttr.dataset.setSourceIdFromAttr;
        var val = nodeAttr.getAttribute(attrName);
        var hiddenInput2 = document.querySelector("#selectedSourceId");
        if (hiddenInput2 && val !== null) {
            hiddenInput2.value = val;
        }
    }
});

// ---------------------------------------------------------------------------
// cache.html: toggle-changed sets hidden input value
// Elements with data-toggle-target="<selector>" update the hidden field value
// ---------------------------------------------------------------------------
document.body.addEventListener("cds-toggle-changed", function (event) {
    var toggle = event.target;
    if (!toggle || !toggle.dataset || !toggle.dataset.toggleTarget) return;
    var hiddenInput = document.querySelector(toggle.dataset.toggleTarget);
    if (hiddenInput) {
        hiddenInput.value = toggle.checked;
    }
});

// ---------------------------------------------------------------------------
// connector/details-page-connector.html: editor save/edit buttons.
// Event delegation keyed by data-* attributes — one listener for all such
// buttons, no inline onclick (CSP-safe).
// ---------------------------------------------------------------------------
document.body.addEventListener("click", function (event) {
    var btn = event.target.closest("[data-toggle-editor-mode]");
    if (!btn) return;
    var editMode = btn.dataset.toggleEditorMode === "edit";
    if (typeof toggleEditorMode === "function") {
        toggleEditorMode(
            editMode,
            btn.dataset.saveBtnRef,
            btn.dataset.editBtnRef,
            btn.dataset.editorId,
        );
    }
});

// ---------------------------------------------------------------------------
// connector/form-create-connector.html: keep the connector code in sync with
// the reference field. The default code carries a `direct:iko:connector:CHANGEME`
// placeholder; as the user types the reference, rewrite the token so they don't
// have to edit the YAML by hand. Tracks the last-applied token per editor.
// ---------------------------------------------------------------------------
document.body.addEventListener("input", function (event) {
    var field = event.target;
    if (!field || field.getAttribute("name") !== "reference") return;
    var editorEl = document.getElementById("connectorCodeEditor");
    if (!editorEl || !editorEl._editor) return;

    var prefix = "direct:iko:connector:";
    var oldToken =
        editorEl._refToken != null ? editorEl._refToken : "CHANGEME";
    var newToken = field.value || "";
    if (newToken === oldToken) return;

    var escaped = oldToken.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    var pattern = new RegExp(escaped ? prefix + escaped : prefix, "g");
    var code = editorEl._editor.getValue();
    var updated = code.replace(pattern, prefix + newToken);
    if (updated !== code) {
        editorEl._editor.setValue(updated, -1);
    }
    editorEl._refToken = newToken;
});

// ---------------------------------------------------------------------------
// logging/list.html: toggle filter panel button
// ---------------------------------------------------------------------------
document.body.addEventListener("click", function (event) {
    var btn = event.target.closest("[data-toggle-logging-filter]");
    if (!btn) return;
    var panel = document.getElementById("logging-filter-panel");
    if (!panel) return;
    panel.classList.toggle("logging-filter-hidden");
});

// ---------------------------------------------------------------------------
// logging/list.html: remove property row and resubmit filter form
// Delegated handler for buttons with data-remove-property-row="true"
// ---------------------------------------------------------------------------
document.body.addEventListener("click", function (event) {
    var btn = event.target.closest("[data-remove-property-row]");
    if (!btn) return;
    var row = btn.closest(".app--logging-property-row");
    if (row) {
        row.remove();
        var form = document.getElementById("logging-filter-form");
        if (form) {
            form.dispatchEvent(new Event("submit"));
        }
    }
});

// ---------------------------------------------------------------------------
// logging/list.html: add property row (delegated, works after HTMX swap)
// ---------------------------------------------------------------------------
document.body.addEventListener("click", function (event) {
    var btn = event.target.closest("#add-property-btn");
    if (!btn) return;
    var rows = document.getElementById("logging-properties-rows");
    if (!rows) return;
    var count = rows.querySelectorAll(".app--logging-property-row").length;

    var div = document.createElement("div");
    div.className = "app--logging-property-row";

    var keyInput = document.createElement("cds-text-input");
    keyInput.setAttribute("name", "properties[" + count + "].key");
    keyInput.setAttribute("placeholder", "Key");
    keyInput.setAttribute("size", "md");

    var valInput = document.createElement("cds-text-input");
    valInput.setAttribute("name", "properties[" + count + "].value");
    valInput.setAttribute("placeholder", "Value");
    valInput.setAttribute("size", "md");

    var removeBtn = document.createElement("cds-button");
    removeBtn.setAttribute("kind", "ghost");
    removeBtn.setAttribute("size", "md");
    removeBtn.setAttribute("type", "button");
    removeBtn.setAttribute("data-remove-property-row", "true");
    removeBtn.innerHTML =
        '<svg slot="icon" focusable="false" preserveAspectRatio="xMidYMid meet"' +
        ' xmlns="http://www.w3.org/2000/svg" fill="currentColor" aria-hidden="true"' +
        ' width="16" height="16" viewBox="0 0 32 32">' +
        '<path d="M12 12H14V24H12zM18 12H20V24H18z"></path>' +
        '<path d="M4 6V8H6V28a2 2 0 002 2H24a2 2 0 002-2V8h2V6zM8 28V8H24V28zM12 2H20V4H12z"></path>' +
        "</svg>";

    div.appendChild(keyInput);
    div.appendChild(valInput);
    div.appendChild(removeBtn);
    rows.appendChild(div);
});

// ---------------------------------------------------------------------------
// logging/list.html: clear filter button
// ---------------------------------------------------------------------------
document.body.addEventListener("click", function (event) {
    var btn = event.target.closest("#clear-filter-btn");
    if (!btn) return;
    var form = document.getElementById("logging-filter-form");
    if (!form) return;
    form.reset();
    form.querySelectorAll("cds-text-input").forEach(function (el) {
        el.value = "";
    });
    form.querySelectorAll("cds-select").forEach(function (el) {
        el.value = "";
    });

    // Reset properties to one empty row using DOM API
    var rows = document.getElementById("logging-properties-rows");
    if (rows) {
        rows.innerHTML = "";

        var div = document.createElement("div");
        div.className = "app--logging-property-row";

        var keyInput = document.createElement("cds-text-input");
        keyInput.setAttribute("name", "properties[0].key");
        keyInput.setAttribute("placeholder", "Key");
        keyInput.setAttribute("size", "md");

        var valInput = document.createElement("cds-text-input");
        valInput.setAttribute("name", "properties[0].value");
        valInput.setAttribute("placeholder", "Value");
        valInput.setAttribute("size", "md");

        var removeBtn = document.createElement("cds-button");
        removeBtn.setAttribute("kind", "ghost");
        removeBtn.setAttribute("size", "md");
        removeBtn.setAttribute("type", "button");
        removeBtn.setAttribute("data-remove-property-row", "true");
        removeBtn.innerHTML =
            '<svg slot="icon" focusable="false" preserveAspectRatio="xMidYMid meet"' +
            ' xmlns="http://www.w3.org/2000/svg" fill="currentColor" aria-hidden="true"' +
            ' width="16" height="16" viewBox="0 0 32 32">' +
            '<path d="M12 12H14V24H12zM18 12H20V24H18z"></path>' +
            '<path d="M4 6V8H6V28a2 2 0 002 2H24a2 2 0 002-2V8h2V6zM8 28V8H24V28zM12 2H20V4H12z"></path>' +
            "</svg>";

        div.appendChild(keyInput);
        div.appendChild(valInput);
        div.appendChild(removeBtn);
        rows.appendChild(div);
    }

    form.dispatchEvent(new Event("submit"));
});

// ---------------------------------------------------------------------------
// logging/list.html: close filter panel when clicking outside
// ---------------------------------------------------------------------------
(function () {
    document.addEventListener("click", function (event) {
        var panel = document.getElementById("logging-filter-panel");
        if (!panel || panel.classList.contains("logging-filter-hidden")) return;
        var toggle = document.getElementById("toggle-filter-btn");
        if (
            panel.contains(event.target) ||
            (toggle && toggle.contains(event.target))
        ) {
            return;
        }
        panel.classList.add("logging-filter-hidden");
    });
})();
