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