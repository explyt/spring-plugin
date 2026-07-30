/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle.message
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Quick-fix that replaces the highlighted type reference (Java `PsiJavaCodeReferenceElement` or Kotlin
 * `KtTypeReference`) with [newTypeFqn] and shortens references, letting the IDE add the import.
 */
class ReplaceTypeQuickFix(private val newTypeFqn: String) : LocalQuickFix {

    override fun getName(): String =
        message("explyt.spring.inspection.replace.type.fix", newTypeFqn.substringAfterLast('.'))

    override fun getFamilyName(): String = message("explyt.spring.inspection.replace.type.fix.family")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        when (val element = descriptor.psiElement) {
            is KtTypeReference -> {
                val newTypeReference = KtPsiFactory(project).createType(newTypeFqn)
                val replaced = element.replaced(newTypeReference)
                ShortenReferencesFacility.getInstance().shorten(replaced)
            }

            is PsiJavaCodeReferenceElement -> replaceJavaReference(project, element)

            is PsiTypeElement -> {
                val reference = element.innermostComponentReferenceElement ?: return
                replaceJavaReference(project, reference)
            }
        }
    }

    private fun replaceJavaReference(project: Project, reference: PsiJavaCodeReferenceElement) {
        val psiClass = JavaPsiFacade.getInstance(project)
            .findClass(newTypeFqn, reference.resolveScope) ?: return
        val newReference = JavaPsiFacade.getInstance(project).elementFactory.createClassReferenceElement(psiClass)
        val replaced = reference.replace(newReference)
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(replaced)
    }
}
