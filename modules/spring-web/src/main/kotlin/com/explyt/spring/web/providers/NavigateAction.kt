/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import java.awt.event.MouseEvent

class NavigateAction<T : PsiElement>(
    text: String,
    private val navigatableLinemarker: LineMarkerInfo<T>,
    private val originalEvent: MouseEvent

) : AnAction(text) {

    override fun actionPerformed(e: AnActionEvent) {
        val psiElement = navigatableLinemarker.element ?: return
        navigatableLinemarker
            .navigationHandler
            .navigate(originalEvent, psiElement)
    }

}