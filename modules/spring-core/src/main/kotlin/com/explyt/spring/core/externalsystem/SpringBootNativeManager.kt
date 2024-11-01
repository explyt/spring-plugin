package com.explyt.spring.core.externalsystem

import com.explyt.spring.core.externalsystem.setting.*
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.util.Function

class SpringBootNativeManager :
    ExternalSystemManager<NativeProjectSettings, SettingsListener, NativeSettings, LocalSettings, NativeExecutionSettings> {

    override fun enhanceRemoteProcessing(parameters: SimpleJavaParameters) =
        throw java.lang.UnsupportedOperationException()

    override fun getSystemId() = SYSTEM_ID

    override fun getSettingsProvider(): Function<Project, NativeSettings> {
        return Function<Project, NativeSettings> { project: Project -> project.getService(NativeSettings::class.java) }
    }

    override fun getLocalSettingsProvider(): Function<Project, LocalSettings> {
        return Function<Project, LocalSettings> { project: Project -> project.getService(LocalSettings::class.java) }
    }

    override fun getExecutionSettingsProvider(): Function<Pair<Project, String>, NativeExecutionSettings> {
        return Function<Pair<Project, String>, NativeExecutionSettings> {
            val project = it.first
            val projectPath = it.second
            val systemSettings = project.getService(NativeSettings::class.java)
            val projectSettings = systemSettings.getLinkedProjectSettings(projectPath)
            val executionSettings = NativeExecutionSettings(project)
            executionSettings.externalProjectMainFilePath = projectSettings?.externalProjectPath
            executionSettings.runConfigurationName = projectSettings?.runConfigurationName
            executionSettings
        }
    }

    override fun getProjectResolverClass() = SpringBeanNativeResolver::class.java

    override fun getTaskManagerClass() = SpringBootNativeTaskManager::class.java

    override fun getExternalProjectDescriptor(): BuildFileChooserDescriptor {
        return BuildFileChooserDescriptor()
    }
}

class BuildFileChooserDescriptor : FileChooserDescriptor(false, true, false, false, false, false)