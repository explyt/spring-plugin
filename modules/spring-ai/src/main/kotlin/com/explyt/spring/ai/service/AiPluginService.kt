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

package com.explyt.spring.ai.service

import com.explyt.chat.api.v1.AgentChatApi
import com.explyt.spring.ai.SpringAiBundle
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.PluginManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import java.time.Instant

private const val GITHUB_WIKI_URL = "https://github.com/explyt/spring-plugin/wiki/Explyt-AI-Actions"

@Service(Service.Level.PROJECT)
class AiPluginService(private val project: Project) {

    fun performPrompt(prompt: String, virtualFile: VirtualFile) {
        performPrompt(prompt, listOf(virtualFile))
    }

    fun performPrompt(prompt: String, virtualFiles: List<VirtualFile>) {
        try {
            AgentChatApi.getInstance(project).createNewChatAndSendRequest(prompt, virtualFiles)
        } catch (e: Throwable) {
            logger.warn("Explyt - error run AI from Spring", e)
            Notification(
                "com.explyt.spring.notification",
                "Install or update Explyt AI plugin.<br> Version 4.1.1 or higher required.",
                NotificationType.INFORMATION
            ).addAction(NotificationAction.create(SpringAiBundle.message("explyt.spring.ai.suggest.about")) {
                BrowserUtil.browse(GITHUB_WIKI_URL)
            }).addAction(NotificationAction.create(SpringAiBundle.message("explyt.spring.ai.suggest.install")) {
                installPlugin(project)
            }).notify(null)
        }
    }

    fun checkAiPlugin(project: Project) {
        val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()
        val instance = PluginManager.getInstance()
        val enabledPlugin = instance.findEnabledPlugin(PluginId.getId("com.explyt.test"))
        if (enabledPlugin != null && StringUtil.compareVersionNumbers(enabledPlugin.version, "4.1.0") >= 0) return
        if (Instant.now().epochSecond < settingsState.aiSuggestInstantSecond) return
        disableAiSuggestion(3600 * 24) //disable for next day
        Notification(
            "com.explyt.spring.notification",
            "Suggested plugin: Explyt AI available.<br>",
            NotificationType.INFORMATION
        ).addAction(NotificationAction.create(SpringAiBundle.message("explyt.spring.ai.suggest.about")) {
            BrowserUtil.browse(GITHUB_WIKI_URL)
        }).addAction(NotificationAction.create(SpringAiBundle.message("explyt.spring.ai.suggest.install")) {
            installPlugin(project)
        }).addAction(NotificationAction.createSimpleExpiring("Don't suggest again") {
            disableAiSuggestion(3600 * 24 * 120) //disable for next 120 days
        }).notify(null)
    }

    private fun installPlugin(project: Project) {
        val explytRepositoryExist = UpdateSettings.getInstance()
            .storedPluginHosts.any { it.contains("explyt", true) }
        val pluginConfigurable = explytRepositoryExist.takeIf { it }?.let {
            Configurable.APPLICATION_CONFIGURABLE.extensionList.find { it.id == "preferences.pluginManager" }
                ?.createConfigurable()
        }
        if (pluginConfigurable != null) {
            ShowSettingsUtil.getInstance().editConfigurable(
                project,
                pluginConfigurable
            ) {
                val javaClass = pluginConfigurable.javaClass
                val method = javaClass.methods.first { it.name == "openMarketplaceTab" }
                method.invoke(pluginConfigurable, "Explyt")
            }
        } else {
            BrowserUtil.browse("https://explyt.ai/en/download")
        }
    }

    private fun disableAiSuggestion(nextSuggestionTime: Int) {
        ApplicationManager.getApplication().invokeLaterOnWriteThread {
            val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()
            val nextInstant = Instant.now().plusSeconds(nextSuggestionTime.toLong())
            settingsState.aiSuggestInstantSecond = nextInstant.epochSecond
        }
    }

    companion object {
        fun getInstance(project: Project): AiPluginService = project.service()

        private val logger = logger<AiPluginService>()
    }
}