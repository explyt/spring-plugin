package com.esprito.spring.core.externalsystem.process

import com.esprito.spring.core.externalsystem.setting.NativeProjectSettings
import com.esprito.spring.core.externalsystem.setting.NativeSettings
import com.esprito.spring.core.externalsystem.utils.Constants
import com.esprito.spring.core.externalsystem.utils.NativeBootUtils
import com.esprito.spring.core.externalsystem.utils.NativeBootUtils.getVirtualFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autolink.ExternalSystemProjectLinkListener
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor


class UnlinkedProjectAware : ExternalSystemUnlinkedProjectAware {
    override val systemId = Constants.SYSTEM_ID

    override fun isBuildFile(project: Project, buildFile: VirtualFile): Boolean {
        if (buildFile.isDirectory) return false
        val mainRootFiles = NativeBootUtils.getMainRootFiles(project)
        return mainRootFiles.contains(buildFile)
    }

    override fun isLinkedProject(project: Project, externalProjectPath: String): Boolean {
        return project.getService(NativeSettings::class.java).getLinkedProjectSettings(externalProjectPath) != null
    }

    override fun subscribe(
        project: Project, listener: ExternalSystemProjectLinkListener, parentDisposable: Disposable
    ) {
        val nativeSettings = project.getService(NativeSettings::class.java)
        nativeSettings.subscribe(UnlinkSettingsListener(listener), parentDisposable)
    }

    override fun linkAndLoadProject(project: Project, externalProjectPath: String) {
        val virtualFile = getVirtualFile(externalProjectPath)
        ProjectOpenProcessor.EXTENSION_POINT_NAME.findExtensionOrFail(SpringBootProjectOpenProcessor::class.java)
            .importProjectAfterwards(project, virtualFile)
    }
}

internal class UnlinkSettingsListener(val listener: ExternalSystemProjectLinkListener) :
    ExternalSystemSettingsListener<NativeProjectSettings?> {
    override fun onProjectsLinked(settings: MutableCollection<NativeProjectSettings?>) {
        settings.filterNotNull().forEach { listener.onProjectLinked(it.externalProjectPath) }
    }

    override fun onProjectsUnlinked(linkedProjectPaths: Set<String>) {
        linkedProjectPaths.forEach { listener.onProjectUnlinked(it) }
    }
}