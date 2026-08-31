/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.hint

import com.explyt.spring.test.ExplytBaseLightTestCase
import com.intellij.codeInsight.codeVision.CodeVisionState.Companion.READY_EMPTY
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.impl.DocumentImpl
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame

class PropertyDebugValueCodeVisionProviderTest : ExplytBaseLightTestCase() {

    fun testReturnsNullForEditorWithoutVirtualFile() {
        val editor = EditorFactory.getInstance().createEditor(DocumentImpl(""))
        try {
            assertNull(editor.virtualFile)
            val provider = PropertyDebugValueCodeVisionProvider()
            assertNull(provider.precomputeOnUiThread(editor))
            assertSame(READY_EMPTY, provider.computeCodeVision(editor, null))
        } finally {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }
}
