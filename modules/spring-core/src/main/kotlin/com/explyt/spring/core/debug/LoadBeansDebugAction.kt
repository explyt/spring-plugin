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

package com.explyt.spring.core.debug

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.externalsystem.action.AttachSpringBootProjectAction
import com.intellij.debugger.engine.JavaValue
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XValue
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import com.intellij.xdebugger.impl.evaluate.XDebuggerEvaluationDialog
import com.sun.jdi.StringReference

class EvaluateDialogDebugAction : AnAction() {
    init {
        templatePresentation.icon = SpringIcons.SpringBean
        templatePresentation.text = "Explyt: Evaluate Spring Context"
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        e.presentation.isEnabledAndVisible = XDebuggerManager.getInstance(project).currentSession
            ?.currentStackFrame?.sourcePosition != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentSession = XDebuggerManager.getInstance(project).currentSession
        if (currentSession?.currentStackFrame == null) {
            Notification(
                "com.explyt.spring.notification",
                "No active stopped break point",
                NotificationType.WARNING
            ).notify(null)
            return
        }
        val fromText = XExpressionImpl.fromText("explyt.Explyt.context", EvaluationMode.EXPRESSION)

        val editorsProvider = currentSession.debugProcess.editorsProvider
        XDebuggerEvaluationDialog(currentSession, editorsProvider, fromText, null, true).show()
    }
}


class LoadBeansDebugAction : AnAction() {
    init {
        templatePresentation.icon = SpringIcons.SpringExplorer
        templatePresentation.text = "Explyt: Load Beans From Debug Session into Tool Window"
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project ?: return
        e.presentation.isEnabledAndVisible = XDebuggerManager.getInstance(project).currentSession
            ?.currentStackFrame?.sourcePosition != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentSession = XDebuggerManager.getInstance(project).currentSession ?: return
        val xSourcePosition = currentSession.currentStackFrame?.sourcePosition ?: return
        currentSession.debugProcess.evaluator?.evaluate(
            "Class.forName(\"com.explyt.spring.boot.bean.reader.InternalHolderContext\").getDeclaredMethod(\"getRawBeanData\").invoke(null)",
            object : XDebuggerEvaluator.XEvaluationCallback {
                override fun evaluated(result: XValue) {
                    ProgressManager.checkCanceled()
                    val runtimeValue = (result as? JavaValue)?.descriptor?.value ?: return
                    val rawBeansData = (runtimeValue as? StringReference)?.value() ?: runtimeValue.toString()
                    println(rawBeansData)
                    ApplicationManager.getApplication().runReadAction {
                        val sessionName = currentSession.sessionName
                        AttachSpringBootProjectAction.attachDebugProject(project, rawBeansData, sessionName)
                    }
                }

                override fun errorOccurred(errorMessage: @NlsContexts.DialogMessage String) {
                    Notification(
                        "com.explyt.spring.notification",
                        "Explyt debugger",
                        errorMessage,
                        NotificationType.ERROR
                    ).notify(project)
                }
            },
            xSourcePosition
        )
    }
}