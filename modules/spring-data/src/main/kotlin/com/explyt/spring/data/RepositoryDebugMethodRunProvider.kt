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

package com.explyt.spring.data

import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.data.util.SpringDataUtil
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.util.InheritanceUtil
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import com.intellij.xdebugger.impl.evaluate.XDebuggerEvaluationDialog
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getUParentForIdentifier

class RepositoryDebugMethodRunProvider : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (!SpringToolRunConfigurationsSettingsState.getInstance().isDebugMode) return null

        val uMethod = getUParentForIdentifier(element) as? UMethod ?: return null
        if (!SpringDataUtil.isSpringDataProject(element.project)) return null
        if (!SpringCoreUtil.isExplytDebug(element.project)) return null
        val uClass = uMethod.getContainingUClass().takeIf { it?.isInterface == true } ?: return null
        val qualifiedName = uClass.qualifiedName ?: return null
        if (!InheritanceUtil.isInheritor(uClass.javaPsi, SpringDataClasses.REPOSITORY)) return null

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            arrayOf(EvaluateInDebugAction(qualifiedName, uMethod.name)),
            { "Explyt: Evaluate in Debug" }
        )
    }
}

private class EvaluateInDebugAction(val qualifiedName: String, val methodName: String)
    : AnAction({ "Explyt: Evaluate in Debug" }, AllIcons.RunConfigurations.TestState.Run) {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val fromText = XExpressionImpl.fromText(
            "com.explyt.spring.boot.bean.reader.ExplytContext.context.getBean($qualifiedName.class).$methodName()", EvaluationMode.EXPRESSION)
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_DEBUG_DATA_METHOD_EVALUATE)

        val debugSession = XDebuggerManager.getInstance(project).currentSession ?: return
        val editorsProvider = debugSession.debugProcess.editorsProvider
        val dialog = XDebuggerEvaluationDialog(debugSession, editorsProvider, fromText, null, true)
        dialog.show()
    }

}