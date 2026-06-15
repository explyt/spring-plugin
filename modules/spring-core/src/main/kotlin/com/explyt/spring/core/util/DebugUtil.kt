/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import com.intellij.xdebugger.impl.evaluate.XDebuggerEvaluationDialog

object DebugUtil {
    fun evaluate(project: Project, textToEval: String) {
        val currentSession = XDebuggerManager.getInstance(project).currentSession
        if (!SpringCoreUtil.isExplytDebug(project) || currentSession?.currentStackFrame == null) {
            Notification(
                "com.explyt.spring.notification",
                "No active stopped break point",
                NotificationType.WARNING
            ).notify(null)
            return
        }

        val fromText = XExpressionImpl.fromText(textToEval, EvaluationMode.EXPRESSION)
        val editorsProvider = currentSession.debugProcess.editorsProvider
        XDebuggerEvaluationDialog(currentSession, editorsProvider, fromText, null, true).show()
    }
}