/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.util.ConfigurationPlaceholder
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiDocumentManager

class ReplaceEnvironmentPlaceholderWithDefaultQuickFix : LocalQuickFix {
    override fun getFamilyName(): String =
        SpringCoreBundle.message("explyt.spring.inspection.environment.placeholder.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val valueRange = ElementManipulators.getValueTextRange(element)
        val placeholder = ConfigurationPlaceholder.parse(valueRange.substring(element.text)) ?: return
        val file = element.containingFile
        val default = placeholder.replacementDefault(file) ?: return
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file) ?: return
        val range = placeholder.range.shiftRight(element.textRange.startOffset + valueRange.startOffset)
        document.replaceString(range.startOffset, range.endOffset, default)
        documentManager.commitDocument(document)
    }
}
