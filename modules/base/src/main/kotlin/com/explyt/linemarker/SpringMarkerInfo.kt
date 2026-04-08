/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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