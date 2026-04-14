/*
 * Copyright © 2024 Explyt Ltd
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

package com.explyt.base

import com.intellij.ide.DataManager
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import java.awt.Component


class SentryErrorReporter : ErrorReportSubmitter() {

    override fun getReportActionText(): String = BaseBundle.message("explyt.base.report.action")

    override fun getReporterAccount(): String = doGetReporterAccount()

    private fun doGetReporterAccount(): String =
        System.getProperties().getProperty("user.name", "anonymous")

    override fun getPrivacyNoticeText(): String = BaseBundle.message("explyt.base.report.notice")

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val context = DataManager.getInstance().getDataContext(parentComponent)
        val project: Project? = CommonDataKeys.PROJECT.getData(context)

        object : Task.Backgroundable(project, BaseBundle.message("explyt.base.report.background")) {
            override fun run(indicator: ProgressIndicator) {
                for (ideaEvent in events) {
                    SentryReporter.withErrorReportTag { scope ->
                        val sentryEvent = ideaEvent.throwable?.let { SentryEvent(it) }
                            ?: SentryEvent().apply {
                                message = Message().apply { message = ideaEvent.throwableText }
                            }

                        sentryEvent.level = SentryLevel.ERROR

                        scope.setExtra("last.action", IdeaLogger.ourLastActionId)
                        additionalInfo?.let { scope.setExtra("event.info", it) }
                        ideaEvent.message?.let { scope.setExtra("event.message", it) }

                        ideaEvent.plugin?.let {
                            scope.setTag("plugin.id", it.pluginId.idString)
                            scope.setTag("plugin.version", it.version)
                        }

                        Sentry.captureEvent(sentryEvent)
                    }
                }

                // Sentry sends async in a background thread
                ApplicationManager.getApplication().invokeLater {
                    consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
                }
            }
        }.queue()
        return true
    }
}
