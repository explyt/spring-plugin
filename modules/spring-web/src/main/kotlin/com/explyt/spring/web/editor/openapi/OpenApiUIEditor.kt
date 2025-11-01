/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.web.editor.openapi

import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class OpenApiUIEditor(textEditor: TextEditor, preview: OpenApiCefBrowser) :
    TextEditorWithPreview(textEditor, preview, "OpenAPI Preview Editor", DEFAULT_LAYOUT) {

    val document = textEditor.editor.document
    val changeEventQueue = ArrayBlockingQueue<Int>(1)

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
            addEvent(0)
        }

        private fun addEvent(retryCount: Int) {
            val added = changeEventQueue.offer(retryCount)
            if (!added) return
            AppExecutorUtil.getAppScheduledExecutorService().schedule(
                { processEvent() }, 1, TimeUnit.SECONDS
            )
        }

        private fun processEvent() {
            val retryCount = changeEventQueue.poll() ?: return
            if (document.isInBulkUpdate) {
                if (retryCount > 10) return
                addEvent(retryCount + 1)
            } else {
                ApplicationManager.getApplication().invokeLater { reloadBrowserHtml() }
            }
        }

        private fun reloadBrowserHtml() {
            runWriteAction {
                FileDocumentManager.getInstance().saveDocument(document)
            }
            val browser = previewEditor as? OpenApiCefBrowser ?: return
            browser.loadHtml("&ts=" + System.currentTimeMillis())
        }
    }
}