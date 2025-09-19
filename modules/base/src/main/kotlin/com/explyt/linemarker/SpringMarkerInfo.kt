/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.linemarker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.MarkupEditorFilter
import com.intellij.openapi.editor.markup.MarkupEditorFilterFactory
import com.intellij.psi.PsiElement
import javax.swing.Icon

class SpringMarkerInfo(
    element: PsiElement,
    icon: Icon,
    tooltipProvider: (PsiElement) -> String,
    private val myActionGroup: DefaultActionGroup,
) : LineMarkerInfo<PsiElement>(
    element,
    element.textRange,
    icon,
    tooltipProvider,
    { _, _ -> }, // to have `hand` cursor on hover
    GutterIconRenderer.Alignment.LEFT,
    { tooltipProvider(element) }
) {

    override fun createGutterRenderer(): LineMarkerGutterIconRenderer<PsiElement> =
        object : LineMarkerGutterIconRenderer<PsiElement>(this) {
            override fun getPopupMenuActions(): ActionGroup = myActionGroup

            override fun getClickAction(): AnAction? = null
        }

    override fun getEditorFilter(): MarkupEditorFilter =
        MarkupEditorFilterFactory.createIsNotDiffFilter()
}