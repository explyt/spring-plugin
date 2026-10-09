/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.quickfix

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory

class MoveControllerBeanNameToRequestMappingQuickFix(
    private val beanName: String,
    private val addRequestMapping: Boolean
) : LocalQuickFix {

    override fun getName(): String = if (addRequestMapping) {
        SpringWebBundle.message("explyt.spring.web.inspection.controller.beanNameAsPath.fix.move", beanName)
    } else {
        SpringWebBundle.message("explyt.spring.web.inspection.controller.beanNameAsPath.fix.remove", beanName)
    }

    override fun getFamilyName(): String =
        SpringWebBundle.message("explyt.spring.web.inspection.controller.beanNameAsPath.fix.family")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val literal = descriptor.psiElement ?: return
        val kotlinStereotype = PsiTreeUtil.getParentOfType(literal, KtAnnotationEntry::class.java)
        if (kotlinStereotype != null) {
            moveInKotlin(project, kotlinStereotype, literal)
            return
        }
        val javaStereotype = PsiTreeUtil.getParentOfType(literal, PsiAnnotation::class.java) ?: return
        moveInJava(project, javaStereotype, literal)
    }

    private fun moveInKotlin(project: Project, stereotype: KtAnnotationEntry, literal: PsiElement) {
        if (addRequestMapping) {
            val psiFactory = KtPsiFactory(project)
            val requestMapping = psiFactory.createAnnotationEntry(requestMappingText(literal))
            val modifierList = stereotype.parent
            val added = modifierList.addAfter(requestMapping, stereotype)
            modifierList.addAfter(psiFactory.createNewLine(), stereotype)
            ShortenReferencesFacility.getInstance().shorten(added as KtElement)
        }
        stereotype.valueArgumentList?.delete()
    }

    private fun moveInJava(project: Project, stereotype: PsiAnnotation, literal: PsiElement) {
        val factory = JavaPsiFacade.getElementFactory(project)
        val styleManager = JavaCodeStyleManager.getInstance(project)
        if (addRequestMapping) {
            val requestMapping = factory.createAnnotationFromText(requestMappingText(literal), stereotype)
            val added = stereotype.parent.addAfter(requestMapping, stereotype)
            styleManager.shortenClassReferences(added)
        }
        val stereotypeName = stereotype.nameReferenceElement?.text ?: return
        stereotype.replace(factory.createAnnotationFromText("@$stereotypeName", stereotype))
    }

    private fun requestMappingText(literal: PsiElement): String = "@${SpringWebClasses.REQUEST_MAPPING}(${literal.text})"
}
