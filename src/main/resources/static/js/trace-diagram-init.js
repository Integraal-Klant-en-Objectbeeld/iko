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

// Hand-rolled vertical git-branch (commit graph) renderer for an ADP debug run. A thin SVG gutter on the
// left draws the branch rails (dots + split/merge curves, one hue per branch); each step is an HTML row to
// the right carrying its label, Carbon cds-tag pills and its timing. Interaction is standard DOM events
// driving the detail modal, whose Details and Exchange tabs share one label/value grid builder.

const SVG_NS = "http://www.w3.org/2000/svg";

const ROW_H = 44;
const LANE_GUTTER = 22;
const LEFT_PAD = 12;
const RAIL_END_PAD = 8;
const LABEL_GAP = 12;
const MAX_LANES = 12;

const LANE_COLORS = [
    "#8a3ffc",
    "#1192e8",
    "#007d79",
    "#9f1853",
    "#fa4d56",
    "#6929c4",
    "#198038",
    "#002d9c",
    "#ee538b",
    "#b28600",
    "#009d9a",
    "#012749",
];

const TYPE_TAG = {
    entrypoint: "purple",
    "endpoint transform": "cyan",
    "transform route": "teal",
    "operation route": "blue",
    response: "cyan",
    mapping: "cool-gray",
    aggregation: "high-contrast",
    "result transform": "cyan",
    result: "green",
    relation: "magenta",
    "relation result": "green",
};

// Step types shown in "simple" mode: the ADP entry, every transform/mapping, and the HTTP call. Advanced
// mode shows every node.
const SIMPLE_TYPES = new Set([
    "ADP_ENTRY",
    "ADP_ENDPOINT_TRANSFORM",
    "CONNECTOR_TRANSFORM",
    "ADP_RESULT_TRANSFORM",
    "RELATION_RESULT_TRANSFORM",
    "RELATION_SOURCE_MAPPING",
    "HTTP",
]);

// Map a node's coarse category + fine subroute to the neutral "supertype" pill (the layer it belongs to).
function supertypeOf(node) {
    if (node.category === "RELATION") return "relation";
    if (node.category === "CONNECTOR") return node.subroute === "response" ? "http" : "connector";
    return "adp";
}

function parseGraph(el) {
    const selector = el.dataset.traceDiagram;
    if (!selector) return null;
    const holder = document.querySelector(selector);
    if (!holder) return null;
    try {
        const graph = JSON.parse(holder.textContent || "{}");
        if (!graph || !Array.isArray(graph.nodes) || graph.nodes.length === 0) {
            return null;
        }
        return graph;
    } catch (e) {
        console.warn("trace-diagram: could not parse embedded graph JSON", e);
        return null;
    }
}

function timingLabel(ms) {
    if (ms == null) return "";
    return ms < 1 ? "< 1 ms" : `${ms} ms`;
}

// A branch-origin node is not drawn as a normal step: the ADP entry becomes a header banner above the
// graph and each relation's RELATION_START becomes an inline "branch header" row. Both leave a minimal
// "start" anchor dot marking where the branch begins.
function isTrunkEntry(n) {
    return n.category === "ENTRY";
}
function isRelationStart(n) {
    return n.category === "RELATION" && n.subroute === "relation";
}
function isStartAnchor(n) {
    return isTrunkEntry(n) || isRelationStart(n);
}

// True for the nodes kept in "simple" mode. The ADP entry (header banner) and relation starts (branch
// headers) are always kept even though they are not "simple" step types.
function isSimpleNode(n) {
    return isTrunkEntry(n) || isRelationStart(n) || SIMPLE_TYPES.has(n.type);
}

// In simple mode, keep only the simple node types and reconnect the surviving nodes: for every dropped
// node, bridge its incoming edges to its outgoing edges so the rails stay connected. Advanced mode returns
// the graph unchanged.
function filterGraph(graph, advanced) {
    if (advanced) return graph;
    const keep = new Set(graph.nodes.filter(isSimpleNode).map((n) => n.name));
    const nodes = graph.nodes.filter((n) => keep.has(n.name));
    const stepIdByName = {};
    nodes.forEach((n) => {
        stepIdByName[n.name] = n.stepId;
    });
    const succ = {};
    graph.links.forEach((l) => {
        (succ[l.source] = succ[l.source] || []).push(l);
    });
    const links = [];
    const seen = new Set();
    nodes.forEach((from) => {
        const stack = (succ[from.name] || []).map((l) => ({ name: l.target, failed: l.status === "FAILED" }));
        const walked = new Set();
        while (stack.length) {
            const cur = stack.pop();
            if (keep.has(cur.name)) {
                const key = `${from.name}->${cur.name}`;
                if (cur.name !== from.name && !seen.has(key)) {
                    seen.add(key);
                    links.push({
                        source: from.name,
                        target: cur.name,
                        status: cur.failed ? "FAILED" : "OK",
                        stepId: stepIdByName[cur.name],
                    });
                }
                continue;
            }
            if (walked.has(cur.name)) continue;
            walked.add(cur.name);
            (succ[cur.name] || []).forEach((l) =>
                stack.push({ name: l.target, failed: cur.failed || l.status === "FAILED" }),
            );
        }
    });
    return { nodes, links, steps: graph.steps };
}

// Ordered render items. In advanced mode a branch origin yields a dot-less header/"start" pair (the
// trunk gets a bare "start" anchor); in simple mode the "start" anchors are dropped — the ADP banner
// stands in for the trunk origin, and a relation's header row itself carries the origin dot
// ("branchHeaderAnchor"). Every other node is a "step".
function buildItems(graph, advanced) {
    const items = [];
    graph.nodes.forEach((n) => {
        if (isRelationStart(n)) {
            if (advanced) {
                items.push({ kind: "branchHeader", node: n });
                items.push({ kind: "anchor", node: n });
            } else {
                items.push({ kind: "branchHeaderAnchor", node: n });
            }
        } else if (isTrunkEntry(n)) {
            if (advanced) items.push({ kind: "anchor", node: n });
        } else {
            items.push({ kind: "step", node: n });
        }
    });
    return items;
}

// Lay items into lanes/rows. Lanes follow branch first-appearance so the trunk is lane 0.
function layoutGraph(items) {
    const laneOf = {};
    let nextLane = 0;
    const placed = {};
    items.forEach((item, row) => {
        const n = item.node;
        if (!(n.branch in laneOf)) {
            laneOf[n.branch] = Math.min(nextLane, MAX_LANES - 1);
            nextLane += 1;
        }
        const lane = laneOf[n.branch];
        item.lane = lane;
        item.row = row;
        item.x = LEFT_PAD + LANE_GUTTER / 2 + lane * LANE_GUTTER;
        item.y = row * ROW_H + ROW_H / 2;
        if (item.kind !== "branchHeader") {
            placed[n.name] = { node: n, lane, row, x: item.x, y: item.y };
        }
    });
    const laneCount = Math.min(nextLane, MAX_LANES);
    return {
        placed,
        railsWidth: LEFT_PAD + laneCount * LANE_GUTTER + RAIL_END_PAD,
        height: items.length * ROW_H,
    };
}

function svgEl(name, attrs) {
    const el = document.createElementNS(SVG_NS, name);
    if (attrs) {
        Object.keys(attrs).forEach((k) => el.setAttribute(k, attrs[k]));
    }
    return el;
}

// A connector from one dot to another. Same lane is a straight vertical line; cross-lane is an S-curve.
function edgePath(x1, y1, x2, y2) {
    if (x1 === x2) return `M${x1},${y1} L${x2},${y2}`;
    const my = (y1 + y2) / 2;
    return `M${x1},${y1} C${x1},${my} ${x2},${my} ${x2},${y2}`;
}

function pill(text, type) {
    const tag = document.createElement("cds-tag");
    tag.setAttribute("type", type);
    tag.setAttribute("size", "sm");
    tag.textContent = String(text).toLowerCase();
    return tag;
}

function statusTag(status) {
    const tag = document.createElement("cds-tag");
    tag.setAttribute("size", "sm");
    tag.setAttribute("type", status === "FAILED" ? "red" : status === "OK" ? "green" : "gray");
    tag.textContent = String(status || "").toLowerCase();
    return tag;
}

// Overall run status: FAILED if any step failed, else IN_PROGRESS if any is unfinished, else OK. The ADP
// entry itself is a synthetic marker that never fails, so its own status is not used here.
function overallStatus(graph) {
    if (graph.nodes.some((n) => n.status === "FAILED")) return "FAILED";
    if (graph.nodes.some((n) => n.status === "IN_PROGRESS")) return "IN_PROGRESS";
    return "OK";
}

// The trunk header banner, built from the ADP entry node: adp name + overall run status + total elapsed.
function buildGraphHeader(entryNode, graph) {
    const step = graph.steps && graph.steps[entryNode.stepId];
    const header = document.createElement("div");
    header.className = "trace-graph-header";
    header.dataset.stepId = entryNode.stepId;
    header.tabIndex = 0;
    header.setAttribute("role", "button");

    const title = document.createElement("span");
    title.className = "trace-graph-header-title";
    title.textContent = `Executed ADP: ${entryNode.entity || entryNode.label || entryNode.name}`;
    header.appendChild(title);

    header.appendChild(statusTag(overallStatus(graph)));

    const timing = timingLabel(step ? step.elapsedMs : null);
    if (timing) {
        const t = document.createElement("span");
        t.className = "trace-graph-header-elapsed";
        t.textContent = timing;
        header.appendChild(t);
    }
    return header;
}

// Build one row for a render item: a "start" anchor, an inline relation "branch header", or a step row.
function buildRow(item, graph, layout) {
    const n = item.node;
    const step = graph.steps && graph.steps[n.stepId];
    const failed = n.status === "FAILED";
    const row = document.createElement("div");
    row.style.height = `${ROW_H}px`;
    row.style.paddingLeft = `${layout.railsWidth + LABEL_GAP}px`;
    row.dataset.branch = n.branch;

    if (item.kind === "anchor") {
        row.className = "trace-row trace-row--start";
        const label = document.createElement("span");
        label.className = "trace-row-label";
        label.textContent = "start";
        row.appendChild(label);
        return row;
    }

    row.dataset.stepId = n.stepId;
    row.tabIndex = 0;
    row.setAttribute("role", "button");

    const label = document.createElement("span");
    label.className = "trace-row-label";

    if (item.kind === "branchHeader" || item.kind === "branchHeaderAnchor") {
        row.className = `trace-row trace-row--branch-header${failed ? " trace-row--failed" : ""}`;
        label.textContent = `Executed Relation: ${n.entity || n.label || n.name}`;
        row.appendChild(label);
        row.appendChild(pill(supertypeOf(n), "gray"));
        if (failed) row.appendChild(pill("failed", "red"));
    } else {
        row.className = `trace-row${failed ? " trace-row--failed" : ""}`;
        label.textContent = n.label || n.name;
        row.appendChild(label);
        row.appendChild(pill(supertypeOf(n), "gray"));
        if (n.subroute) row.appendChild(pill(n.subroute, TYPE_TAG[n.subroute] || "gray"));
        if (n.entity) row.appendChild(pill(n.entity, "outline"));
        if (n.iterations && n.iterations > 1) row.appendChild(pill(`×${n.iterations}`, "high-contrast"));
        if (failed) row.appendChild(pill("failed", "red"));
    }

    const timing = timingLabel(step ? step.elapsedMs : null);
    if (timing) {
        const t = document.createElement("span");
        t.className = "trace-row-timing";
        t.textContent = timing;
        row.appendChild(t);
    }
    return row;
}

function renderGraph(el, graph, advanced) {
    el.textContent = "";
    const items = buildItems(graph, advanced);
    const layout = layoutGraph(items);

    const container = document.createElement("div");
    container.className = "trace-graph";

    const entryNode = graph.nodes.find(isTrunkEntry);
    if (entryNode) container.appendChild(buildGraphHeader(entryNode, graph));

    const flow = document.createElement("div");
    flow.className = "trace-flow";
    flow.style.minHeight = `${layout.height}px`;

    const svg = svgEl("svg", {
        class: "trace-rails",
        width: String(layout.railsWidth),
        height: String(layout.height),
        viewBox: `0 0 ${layout.railsWidth} ${layout.height}`,
        "aria-hidden": "true",
    });
    graph.links.forEach((link) => {
        const from = layout.placed[link.source];
        const to = layout.placed[link.target];
        if (!from || !to) return;
        const failed = link.status === "FAILED";
        const path = svgEl("path", {
            class: `trace-edge${failed ? " trace-edge--failed" : ""}`,
            d: edgePath(from.x, from.y, to.x, to.y),
            fill: "none",
        });
        if (!failed) path.setAttribute("stroke", LANE_COLORS[from.lane % LANE_COLORS.length]);
        path.dataset.branch = to.node.branch;
        svg.appendChild(path);
    });
    items.forEach((item) => {
        if (item.kind === "branchHeader") return;
        const n = item.node;
        const endpoint = n.category === "OUTPUT" || isStartAnchor(n);
        const statusClass =
            n.status === "FAILED"
                ? "trace-node--failed"
                : n.status === "IN_PROGRESS"
                  ? "trace-node--in-progress"
                  : "trace-node--ok";
        const dot = svgEl("circle", {
            class: `trace-node-dot ${statusClass}`,
            cx: String(item.x),
            cy: String(item.y),
            r: endpoint ? "6.5" : "5",
            fill: LANE_COLORS[item.lane % LANE_COLORS.length],
        });
        dot.dataset.branch = n.branch;
        svg.appendChild(dot);
    });
    flow.appendChild(svg);

    const rows = document.createElement("div");
    rows.className = "trace-rows";
    items.forEach((item) => rows.appendChild(buildRow(item, graph, layout)));
    flow.appendChild(rows);

    container.appendChild(flow);
    el.appendChild(container);
    wireInteraction(el, container, graph);
}

function wireInteraction(el, container, graph) {
    const openById = (id) => {
        const step = graph.steps ? graph.steps[id] : null;
        const node = graph.nodes ? graph.nodes.find((n) => n.stepId === id) : null;
        openTraceModal(step, node);
    };
    const openFor = (target) => {
        const row = target.closest("[data-step-id]");
        if (!row) return;
        openById(row.dataset.stepId);
    };
    container.addEventListener("click", (e) => openFor(e.target));
    container.addEventListener("keydown", (e) => {
        if (e.key !== "Enter" && e.key !== " ") return;
        const row = e.target.closest && e.target.closest("[data-step-id]");
        if (!row) return;
        e.preventDefault();
        openById(row.dataset.stepId);
    });
    const setBranchActive = (branch, on) => {
        if (!branch) return;
        el.querySelectorAll(`[data-branch="${cssEscape(branch)}"]`).forEach((node) => {
            node.classList.toggle("trace-branch--active", on);
        });
    };
    const onEnter = (e) => {
        const g = e.target.closest && e.target.closest("[data-branch]");
        if (g) setBranchActive(g.dataset.branch, true);
    };
    const onLeave = (e) => {
        const g = e.target.closest && e.target.closest("[data-branch]");
        if (g) setBranchActive(g.dataset.branch, false);
    };
    container.addEventListener("mouseover", onEnter);
    container.addEventListener("mouseout", onLeave);
    container.addEventListener("focusin", onEnter);
    container.addEventListener("focusout", onLeave);
}

function cssEscape(value) {
    if (window.CSS && window.CSS.escape) return window.CSS.escape(value);
    return String(value).replace(/["\\]/g, "\\$&");
}

// The Flow panel's Simple/Advanced toggle (unchecked = simple, the default).
function isAdvanced() {
    const t = document.getElementById("trace-flow-mode");
    return !!(t && t.checked);
}

function build(el) {
    if (el._traceRendered) return;
    const rect = el.getBoundingClientRect();
    if (rect.width < 1) return;

    const graph = el._traceGraph || parseGraph(el);
    if (!graph) return;
    el._traceGraph = graph;

    const advanced = isAdvanced();
    try {
        renderGraph(el, filterGraph(graph, advanced), advanced);
        el._traceRendered = true;
    } catch (e) {
        console.warn("trace-diagram: could not render flow graph", e);
        showDiagramError(el);
    }
}

function rerender(el) {
    el._traceRendered = false;
    build(el);
}

function showDiagramError(el) {
    el.textContent = "";
    const msg = document.createElement("p");
    msg.className = "trace-diagram-empty";
    msg.textContent =
        "The flow diagram could not be rendered for this run. See the Trace tab for the full step list.";
    el.appendChild(msg);
}

// Re-render every diagram when the Simple/Advanced toggle changes. The toggle is recreated on each HTMX
// swap, so it is re-wired on every init.
function wireToggle() {
    const t = document.getElementById("trace-flow-mode");
    if (!t || t._traceWired) return;
    t._traceWired = true;
    const onChange = () => document.querySelectorAll("[data-trace-diagram]").forEach(rerender);
    t.addEventListener("cds-toggle-changed", onChange);
    t.addEventListener("change", onChange);
}

function initDiagram(el) {
    wireToggle();
    build(el);
    if (el._traceObserver || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(() => build(el));
    observer.observe(el);
    el._traceObserver = observer;
}

function initAll(root) {
    (root || document).querySelectorAll("[data-trace-diagram]").forEach(initDiagram);
}

function disposeDiagram(el) {
    if (el._traceObserver) {
        el._traceObserver.disconnect();
        el._traceObserver = null;
    }
    el._traceGraph = null;
    el._traceRendered = false;
}

function disposeAll(root) {
    (root || document).querySelectorAll("[data-trace-diagram]").forEach(disposeDiagram);
}

// ---- Modal population --------------------------------------------------

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value == null || value === "" ? "—" : String(value);
}

function formatHeaders(headers) {
    if (!headers || Object.keys(headers).length === 0) return "";
    return Object.keys(headers)
        .map((k) => `${k}: ${headers[k]}`)
        .join("\n");
}

// Pretty-print a valid-JSON body; pass anything else (e.g. a Java Map.toString()) through unchanged.
function formatBody(text) {
    if (text == null || text === "") return text;
    try {
        return JSON.stringify(JSON.parse(text), null, 2);
    } catch (e) {
        return text;
    }
}

// Read-only Ace viewer used across the modal grid. Ace fills its container width and owns its own scroll.
// `ace` is a global already strict-CSP-configured by editor-init.js; fall back to plain text if absent.
function mountAce(el, text) {
    if (!el) return;
    const value = text == null || text === "" ? "—" : String(text);
    if (typeof ace === "undefined") {
        el.textContent = value;
        return;
    }
    let editor = el._traceAceEditor;
    if (!editor) {
        editor = ace.edit(el, {
            readOnly: true,
            useWorker: false,
            mode: "ace/mode/text",
            theme: "ace/theme/chrome",
            showPrintMargin: false,
            wrap: true,
            minLines: 2,
            maxLines: 24,
            highlightActiveLine: false,
        });
        el._traceAceEditor = editor;
        if (typeof ResizeObserver !== "undefined") {
            const ro = new ResizeObserver(() => editor.resize());
            ro.observe(el);
        }
    }
    editor.setValue(value, -1);
    editor.resize();
}

function disposeAce(root) {
    (root || document).querySelectorAll(".trace-modal-ace").forEach((el) => {
        if (el._traceAceEditor) {
            el._traceAceEditor.destroy();
            el._traceAceEditor = null;
        }
    });
}

// The single label/value grid builder shared by every modal surface: the Details tab (all step types) and
// both Exchange sections. `rows` is a list of { label, value, code }; code rows render their value in a
// read-only Ace viewer, mounted after the row is attached (Ace needs the container in the DOM to measure).
function buildDetailGrid(container, rows) {
    if (!container) return;
    disposeAce(container);
    container.textContent = "";
    const grid = document.createElement("div");
    grid.className = "trace-modal-details";
    const pending = [];

    rows.forEach((field) => {
        const row = document.createElement("div");
        row.className = "trace-modal-detail-row";
        const key = document.createElement("span");
        key.className = "trace-modal-detail-label";
        key.textContent = field.label;
        const val = document.createElement("div");
        val.className = "trace-modal-detail-value";
        if (field.code) {
            const editor = document.createElement("div");
            editor.className = "trace-modal-ace";
            val.appendChild(editor);
            pending.push({ el: editor, text: field.value == null ? "" : String(field.value) });
        } else {
            val.textContent = field.value == null || field.value === "" ? "—" : String(field.value);
        }
        row.appendChild(key);
        row.appendChild(val);
        grid.appendChild(row);
    });

    container.appendChild(grid);
    pending.forEach((p) => mountAce(p.el, p.text));
}

function stepDetailRows(step) {
    const rows = (step.fields || []).slice();
    if (step.elapsedMs != null) rows.push({ label: "Elapsed", value: `${step.elapsedMs} ms` });
    return rows;
}

// Mirror a step row's pills into the modal header, alongside the status tag.
function buildModalPills(node) {
    const holder = document.getElementById("trace-modal-pills");
    if (!holder) return;
    holder.textContent = "";
    if (!node) return;
    holder.appendChild(pill(supertypeOf(node), "gray"));
    if (node.subroute) holder.appendChild(pill(node.subroute, TYPE_TAG[node.subroute] || "gray"));
    if (node.entity) holder.appendChild(pill(node.entity, "outline"));
    if (node.iterations && node.iterations > 1) holder.appendChild(pill(`×${node.iterations}`, "high-contrast"));
}

function openTraceModal(step, node) {
    const modal = document.getElementById("trace-step-modal");
    if (!modal || !step) return;

    setText("trace-modal-label", step.label);
    buildModalPills(node);

    const status = document.getElementById("trace-modal-status");
    if (status) {
        status.textContent = step.status;
        status.setAttribute("type", step.status === "FAILED" ? "red" : step.status === "OK" ? "green" : "gray");
    }

    buildDetailGrid(document.getElementById("trace-modal-fields"), stepDetailRows(step));

    const input = step.input || {};
    buildDetailGrid(document.getElementById("trace-modal-input-fields"), [
        { label: "Body", value: formatBody(input.body || ""), code: true },
        { label: "Headers", value: formatHeaders(input.headers), code: true },
    ]);

    const outputSection = document.getElementById("trace-modal-output-section");
    if (outputSection) {
        if (step.output) {
            outputSection.removeAttribute("hidden");
            const rows = [];
            if (step.http && step.http.status != null) {
                rows.push({ label: "HTTP status", value: String(step.http.status) });
            }
            rows.push({ label: "Body", value: formatBody(step.output.body || ""), code: true });
            rows.push({ label: "Headers", value: formatHeaders(step.output.headers), code: true });
            buildDetailGrid(document.getElementById("trace-modal-output-fields"), rows);
        } else {
            outputSection.setAttribute("hidden", "");
        }
    }

    const tabs = document.getElementById("trace-modal-tabs");
    if (tabs) tabs.value = "details";
    const detailsPanel = document.getElementById("trace-modal-panel-details");
    const exchangePanel = document.getElementById("trace-modal-panel-exchange");
    if (detailsPanel) detailsPanel.hidden = false;
    if (exchangePanel) exchangePanel.hidden = true;

    modal.setAttribute("open", "");
}

// ---- HTMX lifecycle ----------------------------------------------------

document.addEventListener("DOMContentLoaded", () => initAll(document));
document.addEventListener("htmx:load", () => initAll(document));
document.addEventListener("htmx:afterSwap", () => initAll(document));
document.addEventListener("htmx:afterSettle", () => initAll(document));
document.addEventListener("htmx:beforeSwap", (e) => {
    const root = (e.detail && e.detail.target) || e.target || document;
    disposeAll(root);
    disposeAce(root);
});
