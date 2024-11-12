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

package com.explyt.spring.initializr

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

class ExplytSpringInitializrModuleBuilder : ModuleBuilder() {

    private var wizardStep: SpringInitializrWizardStep? = null

    override fun getModuleType(): ExplytSpringInitializrModuleType {
        return ExplytSpringInitializrModuleType.getInstance()
    }

    override fun getIgnoredSteps(): List<Class<out ModuleWizardStep>> {
        return listOf<Class<out ModuleWizardStep>>(ProjectSettingsStep::class.java)
    }

    override fun createProject(name: String?, path: String?): Project? {
        val zipFilePath = wizardStep?.downloadFullPath ?: return null
        val zipFile = File(zipFilePath)
        val extractDirectory = wizardStep?.projectsDirectory ?: return null
        val extractProject = unzip(zipFile, extractDirectory.toPath()) ?: return null

        ApplicationManager.getApplication().invokeLaterOnWriteThread {
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
                    "explytSpringInitializrNotificationGroup",
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
            "explytSpringInitializrNotificationGroup",
            "Unzipping problem",
            "Folder not exists '${file.path}'",
            NotificationType.ERROR
        )
        notification.notify(ProjectUtil.getActiveProject())
        return null
    }
}