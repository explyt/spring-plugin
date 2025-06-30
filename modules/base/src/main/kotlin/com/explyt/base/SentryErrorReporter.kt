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

import com.intellij.diagnostic.LogMessage
import com.intellij.ide.DataManager
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import io.sentry.SentryClient
import io.sentry.SentryClientFactory
import io.sentry.event.Event
import io.sentry.event.EventBuilder
import io.sentry.event.interfaces.ExceptionInterface
import java.awt.Component


class SentryErrorReporter: com.intellij.openapi.diagnostic.ErrorReportSubmitter() {

    val sentryClient: SentryClient = SentryClientFactory.sentryClient(
        "https://7e30ca42b0df43d3b31837cdb77b38f9@app.glitchtip.com/11916"
    )

    init {
        sentryClient.apply {
            val properties = System.getProperties()
            addTag("plugin.version", PluginContext.pluginVersion)
            addTag("idea.kotlin.plugin.use.k2", properties.getProperty("idea.kotlin.plugin.use.k2", "false"))
            addTag("ide.build", PluginContext.ideBuild)
            addTag("environment.java.version", properties.getProperty("java.version", "unknown"))
            addTag("environment.java.vm.name", properties.getProperty("java.vm.name", "unknown"))
            addTag("environment.java.vendor.version", properties.getProperty("java.vendor.version", "unknown"))
            environment = properties.getProperty("os.name", "unknown") + " " + properties.getProperty("os.version", "unknown")
            release = PluginContext.pluginVersion
        }
    }

    override fun getReportActionText(): String = BaseBundle.message("explyt.base.report.action")

    override fun submit(events: Array<out IdeaLoggingEvent>, additionalInfo: String?, parentComponent: Component, consumer: Consumer<in SubmittedReportInfo>): Boolean {
        val context = DataManager.getInstance().getDataContext(parentComponent)
        val project: Project? = CommonDataKeys.PROJECT.getData(context)

        object : Task.Backgroundable(project, BaseBundle.message("explyt.base.report.background")) {
            override fun run(indicator: ProgressIndicator) {
                for (ideaEvent in events) {
                    val eventBuilder = EventBuilder()
                        .withMessage(ideaEvent.throwable.message)
                        .withLevel(Event.Level.ERROR)
                        .withSentryInterface(ExceptionInterface(ideaEvent.throwable))
                        .withExtra("last_action", IdeaLogger.ourLastActionId)
                    /* TODO:
                        it seems last action is not actual for the moment of submitting the issue.
                        It is better to track for last actions and put them into breadcrumbs,
                        in that case we have a chance to find out reproduce path.
                     */

                    additionalInfo?.let {
                        eventBuilder.withExtra("event.info", it)
                    }
                    ideaEvent.message?.let {
                        eventBuilder.withExtra("event.message", it)
                    }
                    ideaEvent.attachments.forEach {
                        eventBuilder.withExtra(it.name, it.encodedBytes)
                    }
                    ideaEvent.plugin?.let {
                        eventBuilder.withTag("event.plugin.id", it.pluginId.idString)
                        eventBuilder.withTag("event.plugin.version", it.version)
                    }
                    // TODO: find non internal api to retrieve exception time
                    (ideaEvent.data as? LogMessage)?.let {
                        eventBuilder.withTimestamp(it.date)
                    }

                    sentryClient.sendEvent(eventBuilder)
                }
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