htmx.config.allowNestedOobSwaps = false;

document.body.addEventListener("close-modal", () => {
    document.querySelector("#modal-container cds-modal")?.toggleAttribute("open");
});

document.body.addEventListener("htmx:afterSwap", (e) => {
    if (e.target.id === "view-panel" && window.hljs) {
        document.querySelectorAll("#view-panel pre code").forEach(block => hljs.highlightElement(block));
    }
});