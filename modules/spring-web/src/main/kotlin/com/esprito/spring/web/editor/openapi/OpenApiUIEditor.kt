package com.esprito.spring.web.editor.openapi

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview

class OpenApiUIEditor(textEditor: TextEditor, preview: FileEditor) :
    TextEditorWithPreview(textEditor, preview, "OpenAPI Preview Editor", Layout.SHOW_EDITOR_AND_PREVIEW) {
}