package com.esprito.spring.initializr

import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.projectWizard.ProjectSettingsStep
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.ZipUtil
import java.io.File
import java.io.IOException
import java.nio.file.Path

class EspritoSpringInitializrModuleBuilder : ModuleBuilder() {

    private var wizardStep: SpringInitializrWizardStep? = null

    override fun getModuleType(): EspritoSpringInitializrModuleType {
        return EspritoSpringInitializrModuleType.getInstance()
    }

    override fun getIgnoredSteps(): List<Class<out ModuleWizardStep>> {
        return listOf<Class<out ModuleWizardStep>>(ProjectSettingsStep::class.java)
    }

    override fun createProject(name: String?, path: String?): Project? {
        val zipFilePath = wizardStep?.downloadFullPath ?: return null
        val zipFile = File(zipFilePath)
        val extractDirectory = wizardStep?.projectsDirectory ?: return null
        val extractProject = unzip(zipFile, extractDirectory.toPath()) ?: return null

        ApplicationManager.getApplication().invokeLater {
            zipFile.delete()
            ProjectManagerEx.getInstanceEx().loadAndOpenProject(extractProject)
        }
        return null
    }

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep? {
        val name = wizardStep?.projectsDirectory?.name
        if (!name.isNullOrBlank() && settingsStep is ProjectSettingsStep) {
            settingsStep.setNameValue(name)
            settingsStep.setModuleName(name)
        }
        return super.modifySettingsStep(settingsStep)
    }

    override fun getCustomOptionsStep(context: WizardContext, parentDisposable: Disposable): ModuleWizardStep {
        wizardStep = SpringInitializrWizardStep(context)
        return wizardStep as SpringInitializrWizardStep
    }

    private fun unzip(zip: File, extractDirectory: Path): String? {
        val zipPath = zip.toPath()
        try {
            ZipUtil.extract(zipPath, extractDirectory, null)
        } catch (e: IOException) {
            val notification = e.message?.let {
                Notification(
                    "espritoSpringInitializrNotificationGroup",
                    "Unzipping problem",
                    it,
                    NotificationType.ERROR
                )
            }
            notification?.notify(ProjectUtil.getActiveProject())

        }

        val zipDirectory = extractDirectory.resolve(FileUtil.getNameWithoutExtension(zip))
        val file = File(zipDirectory.toString())
        if (file.isDirectory) {
            return file.canonicalPath
        }

        val notification = Notification(
            "espritoSpringInitializrNotificationGroup",
            "Unzipping problem",
            "Folder not exists '${file.path}'",
            NotificationType.ERROR
        )
        notification.notify(ProjectUtil.getActiveProject())
        return null
    }
}