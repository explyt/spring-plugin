/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.editor.openapi

import com.explyt.spring.test.ExplytBaseLightTestCase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.DocumentUtil

class OpenApiDocumentSaveTest : ExplytBaseLightTestCase() {

    private var reloads = 0

    fun testChangeOutsideBulkUpdateIsSavedAndReloaded() {
        val document = openApiDocument()

        write { document.insertString(0, "# edited\n") }

        assertFalse("the change must be saved", isUnsaved(document))
        assertEquals(1, reloads)
    }

    fun testBulkUpdateIsSavedOnceAfterItFinishes() {
        val document = openApiDocument()
        var unsavedInsideBulkUpdate = false

        write {
            DocumentUtil.executeInBulk(document) {
                document.insertString(0, "# first\n")
                unsavedInsideBulkUpdate = isUnsaved(document)
                document.insertString(0, "# second\n")
            }
        }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        assertTrue("the document must not be saved inside a bulk update", unsavedInsideBulkUpdate)
        assertFalse("the bulk update must be saved after it finishes", isUnsaved(document))
        assertEquals(1, reloads)
    }

    fun testBulkUpdateWithoutChangesIsNotReloaded() {
        val document = openApiDocument()

        write { DocumentUtil.executeInBulk(document, NO_CHANGE) }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        assertEquals(0, reloads)
    }

    private fun openApiDocument(): Document {
        myFixture.configureByText("openapi.yaml", "openapi: 3.0.1\ninfo:\n  title: Pets\n")
        val document = myFixture.editor.document
        assertFalse(isUnsaved(document))
        document.saveOnChange(testRootDisposable) { reloads++ }
        return document
    }

    private fun isUnsaved(document: Document) = FileDocumentManager.getInstance().isDocumentUnsaved(document)

    private fun write(action: () -> Unit) = WriteCommandAction.runWriteCommandAction(project, action)

    companion object {
        private val NO_CHANGE = Runnable {}
    }
}
