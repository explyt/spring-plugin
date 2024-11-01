package com.explyt.spring.web.editor.openapi

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.util.Key

class OpenApiUIEditor(textEditor: TextEditor, preview: OpenApiCefBrowser) :
    TextEditorWithPreview(textEditor, preview, "OpenAPI Preview Editor", DEFAULT_LAYOUT) {

    init {
        textEditor.putUserData(PARENT_EDITOR_KEY, this)
        preview.putUserData(PARENT_EDITOR_KEY, this)
    }

    fun showPreviewFor(tag: String, operationId: String) {
        if (getLayout() == Layout.SHOW_EDITOR) {
            setLayout(layout = DEFAULT_LAYOUT)
        }

        val browser = previewEditor as? OpenApiCefBrowser ?: return
        browser.loadHtml("#/$tag/$operationId")
        Thread.sleep(50) //FIXME: why doesn't work for the first time?
        browser.loadHtml("#/$tag/$operationId")
    }

    companion object {
        fun from(editor: FileEditor) =
            when (editor) {
                is OpenApiUIEditor -> editor
                else -> editor.getUserData(PARENT_EDITOR_KEY)
            }

        val PARENT_EDITOR_KEY = Key.create<OpenApiUIEditor>("parentEditorKey")

        private val DEFAULT_LAYOUT = Layout.SHOW_EDITOR_AND_PREVIEW
    }
}