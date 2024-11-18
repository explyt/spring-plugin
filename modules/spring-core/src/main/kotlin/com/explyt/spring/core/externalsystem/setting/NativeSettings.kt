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

package com.explyt.spring.core.externalsystem.setting

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.externalSystem.settings.DelegatingExternalSystemSettingsListener
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.XCollection
import java.nio.file.Path
import java.util.*

@Service(Service.Level.PROJECT)
@State(name = "SpringBootNativeSettings", storages = [Storage("SpringBootNativeSettings.xml")])
class NativeSettings(project: Project) :
    AbstractExternalSystemSettings<NativeSettings, NativeProjectSettings, SettingsListener>(
        SettingsListener.TOPIC,
        project
    )
    , PersistentStateComponent<SystemSettingsState> {


    override fun copyExtraSettingsFrom(settings: NativeSettings) {}

    override fun checkSettings(old: NativeProjectSettings, current: NativeProjectSettings) {}

    override fun loadState(state: SystemSettingsState) {
        super.loadState(state)
    }

    override fun getState(): SystemSettingsState {
        val state = SystemSettingsState()
        fillState(state)
        return state
    }

    override fun getLinkedProjectSettings(projectPath: String): NativeProjectSettings? {
        val projectAbsolutePath = Path.of(projectPath).toAbsolutePath()
        val projectSettings: NativeProjectSettings? = super.getLinkedProjectSettings(projectPath)
        if (projectSettings == null) {
            for (setting in linkedProjectsSettings) {
                val settingPath = Path.of(setting.externalProjectPath).toAbsolutePath()
                if (FileUtil.isAncestor(settingPath.toFile(), projectAbsolutePath.toFile(), false)) {
                    return setting
                }
            }
        }
        return projectSettings
    }

    override fun subscribe(
        listener: ExternalSystemSettingsListener<NativeProjectSettings>, parentDisposable: Disposable
    ) = doSubscribe(DelegatingNativeSettingsListenerAdapter(listener), parentDisposable)

}

class SystemSettingsState : AbstractExternalSystemSettings.State<NativeProjectSettings> {
    private val projectSettings: MutableSet<NativeProjectSettings> = TreeSet<NativeProjectSettings>()

    @XCollection(elementTypes = [NativeProjectSettings::class])
    override fun getLinkedExternalProjectsSettings(): Set<NativeProjectSettings> {
        return projectSettings
    }

    override fun setLinkedExternalProjectsSettings(settings: Set<NativeProjectSettings>?) {
        if (settings != null) {
            projectSettings.addAll(settings)
        }
    }
}

interface SettingsListener : ExternalSystemSettingsListener<NativeProjectSettings> {

    companion object {
        val TOPIC = Topic(SettingsListener::class.java, Topic.BroadcastDirection.NONE)
    }
}

class DelegatingNativeSettingsListenerAdapter
    (delegate: ExternalSystemSettingsListener<NativeProjectSettings>) :
    DelegatingExternalSystemSettingsListener<NativeProjectSettings>(delegate), SettingsListener
