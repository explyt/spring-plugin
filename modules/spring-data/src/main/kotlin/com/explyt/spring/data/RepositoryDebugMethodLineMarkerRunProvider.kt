/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data

import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.util.DebugUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.data.util.SpringDataUtil
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getUParentForIdentifier

class RepositoryDebugMethodLineMarkerRunProvider : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (!SpringToolRunConfigurationsSettingsState.getInstance().isDebugMode) return null

        val uMethod = getUParentForIdentifier(element) as? UMethod ?: return null
        if (!SpringDataUtil.isSpringDataProject(element.project)) return null
        if (!SpringCoreUtil.isExplytDebug(element.project)) return null
        val uClass = uMethod.getContainingUClass().takeIf { it?.isInterface == true } ?: return null
        val qualifiedName = uClass.qualifiedName ?: return null
        if (!InheritanceUtil.isInheritor(uClass.javaPsi, SpringDataClasses.REPOSITORY)) return null
        val language = uClass.javaPsi.language
            .takeIf { it == JavaLanguage.INSTANCE || it == KotlinLanguage.INSTANCE} ?: return null
        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            arrayOf(EvaluateInDebugAction(qualifiedName, uMethod.name, language)),
            { "Explyt: Evaluate in Debug" }
        )
    }
}

private class EvaluateInDebugAction(val qualifiedName: String, val methodName: String, val language: Language)
    : AnAction({ "Explyt: Evaluate in Debug" }, AllIcons.RunConfigurations.TestState.Run) {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val className = "$qualifiedName.class"
        DebugUtil.evaluate(project, "explyt.Explyt.getBean($className).$methodName()")
    }

}