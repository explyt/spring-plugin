/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.linemarker.SpringMarkerInfo
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.NativeSearchService
import com.explyt.spring.core.util.DebugUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getUParentForIdentifier

class DebugBeanDefinitionLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uClass = getUParentForIdentifier(element) as? UClass ?: return null
        val psiClass = uClass.javaPsi
        val qualifiedName = psiClass.qualifiedName ?: return null
        val project = psiClass.project
        if (!SpringCoreUtil.isExplytDebug(project)) return null
        val psiBeans = NativeSearchService.getInstance(project).getAllActiveBeans()
        if (psiBeans.any { it.psiClass.qualifiedName == qualifiedName }) {
            val sourcePsiElement = getSourcePsiElement(uClass) ?: return null
            val actionGroup = DefaultActionGroup()
            actionGroup.add(EvaluateBeanDefinitionAction(qualifiedName))
            actionGroup.add(EvaluateBeanAction(qualifiedName))
            return SpringMarkerInfo(
                sourcePsiElement,
                AllIcons.Actions.StartDebugger,
                { "Explyt Spring Debugger Actions" },
                actionGroup
            )
        }
        return null
    }

    private fun getSourcePsiElement(uClass: UClass): PsiElement? {
        val componentAnno = uClass.uAnnotations
            .firstOrNull { it.javaPsi?.isMetaAnnotatedByOrSelf(SpringCoreClasses.COMPONENT) == true }
            ?.sourcePsi
        if (componentAnno != null) {
            return componentAnno
        }
        val springAnno = uClass.uAnnotations
            .firstOrNull { it.qualifiedName?.contains("com.spring") == true }
            ?.sourcePsi
        if (springAnno != null) {
            return springAnno
        }
        return uClass.uastAnchor?.sourcePsi
    }
}

private class EvaluateBeanDefinitionAction(val qualifiedClassName: String) :
    AnAction("Evaluate Expression: Get Bean Definition") {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val psiClasses = NativeSearchService.getInstance(project).getAllActiveBeans()
        val beanName = psiClasses.firstOrNull { it.psiClass.qualifiedName == qualifiedClassName }?.name
        val textToEval = "explyt.Explyt.getBeanDefinition(\"${beanName}\")"
        DebugUtil.evaluate(project, textToEval)
    }
}

private class EvaluateBeanAction(val qualifiedClassName: String) : AnAction("Evaluate Expression: Get Bean") {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val textToEval = "explyt.Explyt.getBean(${qualifiedClassName}.class)"
        DebugUtil.evaluate(project, textToEval)
    }
}