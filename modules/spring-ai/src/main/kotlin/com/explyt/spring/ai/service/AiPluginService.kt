/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.service

import com.explyt.plugin.PluginIds
import com.explyt.spring.ai.SpringAiBundle
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import java.time.Instant
import kotlin.reflect.KCallable
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMembers

private const val GITHUB_WIKI_URL = "https://github.com/explyt/spring-plugin/wiki/Explyt-AI-Actions"

@Service(Service.Level.PROJECT)
class AiPluginService(private val project: Project) {

    fun performPrompt(prompt: String, virtualFile: VirtualFile) {
        performPrompt(prompt, listOf(virtualFile))
    }

    fun performPrompt(prompt: String, virtualFiles: List<VirtualFile>) {
        try {
            val klass = Class.forName("com.explyt.chat.api.v1.AgentChatApi").kotlin
            val companionObject = klass.companionObject!!
            val getInstanceMember = companionObject.declaredMembers.first { it.name == "getInstance" }
            val instanceChat = getInstanceMember.call(klass.companionObjectInstance, project)!!
            val newChatMember = instanceChat::class.declaredMembers.first { isNewChatMember(it) }
            newChatMember.call(instanceChat, prompt, virtualFiles.toList(), true)
            //AgentChatApi.getInstance(project).createNewChatAndSendRequest(prompt, virtualFiles)
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

    private fun isNewChatMember(callable: KCallable<*>): Boolean {
        if (callable.name != "createNewChatAndSendRequest") return false
        if (callable.parameters.size < 3) return false
        if (callable.parameters[1].name != "prompt") return false
        if (callable.parameters[2].name != "files") return false
        return true
    }

    fun checkAiPlugin(project: Project) {
        val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()
        val enabledPlugin = PluginIds.EXPLYT.findEnabled()
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
        val pluginConfigurable = Configurable.APPLICATION_CONFIGURABLE.extensionList
            .find { it.id == "preferences.pluginManager" }?.createConfigurable()

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