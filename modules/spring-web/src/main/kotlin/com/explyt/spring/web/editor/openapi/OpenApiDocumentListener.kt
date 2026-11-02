/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.editor.openapi

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer

internal fun Document.saveOnChange(parent: Disposable, afterSave: () -> Unit) {
    addDocumentListener(OpenApiDocumentListener(parent, afterSave), parent)
}

private class OpenApiDocumentListener(
    private val parent: Disposable,
    private val afterSave: () -> Unit,
) : DocumentListener {

    private var changedInBulkUpdate = false

    override fun documentChanged(event: DocumentEvent) {
        if (event.document.isInBulkUpdate) {
            changedInBulkUpdate = true
            return
        }
        save(event.document)
    }

    override fun bulkUpdateFinished(document: Document) {
        if (!changedInBulkUpdate) return
        changedInBulkUpdate = false
        ApplicationManager.getApplication().invokeLater({ save(document) }, Condition<Any> { Disposer.isDisposed(parent) })
    }

    private fun save(document: Document) {
        runWriteAction { FileDocumentManager.getInstance().saveDocument(document) }
        afterSave()
    }
}
