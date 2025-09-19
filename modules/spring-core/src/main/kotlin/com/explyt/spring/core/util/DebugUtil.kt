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