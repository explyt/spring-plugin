/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.editor.openapi

import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.util.Key

class OpenApiUIEditor(textEditor: TextEditor, preview: OpenApiCefBrowser) :
    TextEditorWithPreview(textEditor, preview, "OpenAPI Preview Editor", DEFAULT_LAYOUT) {

    val document = textEditor.editor.document

    init {
        textEditor.putUserData(PARENT_EDITOR_KEY, this)
        preview.putUserData(PARENT_EDITOR_KEY, this)
        document.addDocumentListener(OpenApiDocumentListener(), textEditor)
    }

    fun showPreview(anchor: String = "", layout: Layout = DEFAULT_LAYOUT) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_OPENAPI_ENDPOINT_OPEN_IN_SWAGGER)

        if (isModified) {
            runWriteAction { FileDocumentManager.getInstance().saveDocument(document) }
        }

        if (getLayout() != layout) {
            setLayout(layout = layout)
        }

        val browser = previewEditor as? OpenApiCefBrowser ?: return
        browser.loadHtml(anchor)
    }

    fun showPreviewFor(tag: String, operationId: String, layout: Layout = DEFAULT_LAYOUT) {
        showPreview("?anchor=/$tag/$operationId", layout)
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

    private inner class OpenApiDocumentListener : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            runWriteAction {
                FileDocumentManager.getInstance().saveDocument(document)
            }
            val browser = previewEditor as? OpenApiCefBrowser ?: return
            browser.loadHtml("&ts=" + System.currentTimeMillis())
        }
    }
}