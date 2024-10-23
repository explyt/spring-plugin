package com.esprito.spring.core.externalsystem.setting

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
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
        SettingsListener.TOPIC, project
    ), PersistentStateComponent<SystemSettingsState> {


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
        listener: ExternalSystemSettingsListener<NativeProjectSettings?>, parentDisposable: Disposable
    ) {
    }
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

interface SettingsListener : ExternalSystemSettingsListener<NativeProjectSettings?> {

    companion object {
        val TOPIC = Topic(SettingsListener::class.java, Topic.BroadcastDirection.NONE)
    }
}
