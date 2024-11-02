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

import com.intellij.diagnostic.IdeaReportingEvent
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import java.awt.Component


class SentryErrorReporter: com.intellij.openapi.diagnostic.ErrorReportSubmitter() {
    init {
        Sentry.init { options ->
            options.dsn = "https://2ce963913d1f6bc5d2a4f4dd9ee56c8c@o4507647290310656.ingest.de.sentry.io/4507647358468176"
            // Set tracesSampleRate to 1.0 to capture 100% of transactions for performance monitoring.
            // We recommend adjusting this value in production.
            options.tracesSampleRate = 1.0
            // When first trying Sentry it's good to see what the SDK is doing:
            options.isDebug = true
            options.tags += "ide.build" to ApplicationInfo.getInstance().build.asString()
            if (pluginDescriptor is IdeaPluginDescriptor) {
                options.tags += "plugin.build" to (pluginDescriptor as IdeaPluginDescriptor).version
            }
        }
    }

    override fun getReportActionText(): String = BaseBundle.message("explyt.base.report.action")

    override fun submit(events: Array<out IdeaLoggingEvent>, additionalInfo: String?, parentComponent: Component, consumer: Consumer<in SubmittedReportInfo>): Boolean {
        val context = DataManager.getInstance().getDataContext(parentComponent)
        val project: Project? = CommonDataKeys.PROJECT.getData(context)

        object : Task.Backgroundable(project, BaseBundle.message("explyt.base.report.background")) {
            override fun run(indicator: ProgressIndicator) {
                for (ideaEvent in events) {
                    val event = SentryEvent()
                    event.level = SentryLevel.ERROR
                    // set server name to empty to avoid tracking personal data
                    event.serverName = ""

                    // this is the tricky part
                    // ideaEvent.throwable is a com.intellij.diagnostic.IdeaReportingEvent.TextBasedThrowable
                    // This is a wrapper and is only providing the original stack trace via 'printStackTrace(...)',
                    // but not via 'getStackTrace()'.
                    //
                    // Sentry accesses Throwable.getStackTrace(),
                    // So, we workaround this by retrieving the original exception from the data property
                    if (ideaEvent is IdeaReportingEvent) {
                        event.throwable = ideaEvent.data.throwable
                        event.release = ideaEvent.plugin?.version
                    } else {
                        // ignoring this ideaEvent, you might not want to do this
                        event.throwable = ideaEvent.throwable
                    }

                    event.setExtra("last_action", IdeaLogger.ourLastActionId)
                    Sentry.captureEvent(event)
                }
                // might be useful to debug the exception

                // by default, Sentry is sending async in a background thread

                ApplicationManager.getApplication().invokeLater {
                    // we're a bit lazy here.
                    // Alternatively, we could add a listener to the sentry client
                    // to be notified if the message was successfully send
                    consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
                }
            }

        }.queue()
        return true
    }
}