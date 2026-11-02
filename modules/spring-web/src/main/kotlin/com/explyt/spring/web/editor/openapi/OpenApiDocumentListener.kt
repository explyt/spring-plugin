/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.editor.openapi

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager

internal fun Document.saveOnChange(parent: Disposable, afterSave: () -> Unit) {
    addDocumentListener(OpenApiDocumentListener(afterSave), parent)
}

private class OpenApiDocumentListener(private val afterSave: () -> Unit) : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        runWriteAction {
            FileDocumentManager.getInstance().saveDocument(event.document)
        }
        afterSave()
    }
}
