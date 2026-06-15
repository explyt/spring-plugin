/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.autoconfigure

import com.explyt.spring.core.autoconfigure.language.AutoConfigurationImportsFileType
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement

class AutoConfigureImportsPropertiesInspectionSuppressor : InspectionSuppressor {
    override fun isSuppressedFor(element: PsiElement, toolId: String) =
        AutoConfigurationImportsFileType.isMyFileType(element.containingFile.virtualFile)

    override fun getSuppressActions(element: PsiElement?, toolId: String) = emptyArray<SuppressQuickFix>()
}