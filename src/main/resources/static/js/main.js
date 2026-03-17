/* Responsive side navigation */
(function () {
    const BREAKPOINT = 1056;
    const mql = window.matchMedia(`(min-width: ${BREAKPOINT}px)`);

    function applyLayout(wide) {
        const nav = document.getElementById("side-nav");
        if (!nav) return;

        if (wide) {
            nav.setAttribute("expanded", "");
        } else {
            nav.removeAttribute("expanded");
        }
    }

    mql.addEventListener("change", function (e) {
        applyLayout(e.matches);
    });

    document.addEventListener("DOMContentLoaded", function () {
        applyLayout(mql.matches);

        document.addEventListener(
            "cds-header-menu-button-toggled",
            function () {
                var nav = document.getElementById("side-nav");
                if (!nav) return;

                if (nav.hasAttribute("expanded")) {
                    nav.removeAttribute("expanded");
                } else {
                    nav.setAttribute("expanded", "");
                }
            },
        );
    });
})();

function toggleEditorMode(editMode, saveBtn, editBtn, editorEl) {
    const saveBtnElement = document.getElementById(saveBtn);
    const editBtnElement = document.getElementById(editBtn);
    const editorElElement = document.getElementById(editorEl);

    if (editMode) {
        editBtnElement.style.display = 'none';
        saveBtnElement.style.display = 'block';
        editorElElement.setAttribute('data-readonly', 'false');
    } else {
        saveBtnElement.style.display = 'none';
        editBtnElement.style.display = 'block';
        editorElElement.setAttribute('data-readonly', 'true');
    }

    const editor = editorElElement._monacoEditor;
    if (editor) {
        editor.updateOptions({ readOnly: !editMode });
    }
}