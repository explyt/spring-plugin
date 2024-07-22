package com.esprito.base

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
import com.intellij.openapi.ui.Messages
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
                options.tags += "ide.build" to (pluginDescriptor as IdeaPluginDescriptor).version
            }
        }
    }

    override fun getReportActionText(): String = BaseBundle.message("esprito.base.report.action")

    override fun submit(events: Array<out IdeaLoggingEvent>, additionalInfo: String?, parentComponent: Component, consumer: Consumer<in SubmittedReportInfo>): Boolean {
        val context = DataManager.getInstance().getDataContext(parentComponent)
        val project: Project? = CommonDataKeys.PROJECT.getData(context)

        object : Task.Backgroundable(project, "Sending Error Report") {
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
                    Messages.showInfoMessage(parentComponent, "Thank you for submitting your report!", "Error Report")
                    consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
                }
            }

        }.queue()
        return true
    }
}