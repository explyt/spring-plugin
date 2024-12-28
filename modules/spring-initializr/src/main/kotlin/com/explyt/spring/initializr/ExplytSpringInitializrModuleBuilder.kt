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

import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.ZipUtil
import java.io.File
import java.io.IOException
import java.nio.file.Path

class ExplytSpringInitializrModuleBuilder : ModuleBuilder() {

    private var wizardStep: SpringInitializrWizardStep? = null
    private val logger = Logger.getInstance(ExplytSpringInitializrModuleBuilder::class.java)

    override fun getModuleType(): ExplytSpringInitializrModuleType {
        return ExplytSpringInitializrModuleType.getInstance()
    }

    override fun getIgnoredSteps(): List<Class<out ModuleWizardStep>> {
        return listOf<Class<out ModuleWizardStep>>(ProjectSettingsStep::class.java)
    }

    override fun createProject(name: String, path: String): Project? {
        logger.info("Start creating project. Name: $name, path: $path")

        val zipFilePath = wizardStep?.downloadFullPath
        if (zipFilePath == null) {
            logger.warn("Download is null. Cannot proceed with project creation.")
            return null
        }
        logger.info("Download file path: $zipFilePath")

        val zipFile = File(zipFilePath)
        if (!zipFile.exists()) {
            logger.warn("Download file does not exist: $zipFilePath")
            return null
        }
        val extractDirectory = wizardStep?.projectsDirectory
        if (extractDirectory == null) {
            logger.warn("Extract directory is null. Cannot proceed with project creation.")
            return null
        }
        logger.info("Extract directory: ${extractDirectory.absolutePath}")

        val extractProject = try {
            unzip(zipFile, extractDirectory.toPath())
        } catch (e: Exception) {
            logger.error("Failed to unzip file: $zipFilePath", e)
            return null
        }

        if (extractProject == null) {
            logger.warn("Unzipping returned null. Cannot proceed with project creation.")
            return null
        }
        logger.info("Project extracted successfully to: $extractProject")

        ApplicationManager.getApplication().invokeLaterOnWriteThread {
            val deleted = zipFile.delete()
            if (deleted) {
                logger.info("Deleted ZIP file: $zipFilePath")
            } else {
                logger.warn("Failed to delete ZIP file: $zipFilePath")
            }
            ProjectManagerEx.getInstanceEx().loadAndOpenProject(extractProject)
            logger.info("Project opened successfully: $extractProject")
            StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_INITIALIZR_OPEN_PROJECT)
        }
        logger.info("Project creation process completed.")
        return null
    }

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep? {
        val name = wizardStep?.projectsDirectory?.name
        if (!name.isNullOrBlank() && settingsStep is ProjectSettingsStep) {
            settingsStep.setNameValue(name)
            settingsStep.setModuleName(name)
            StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_INITIALIZR_MODIFY_SETTING)
        }
        return super.modifySettingsStep(settingsStep)
    }

    override fun getCustomOptionsStep(context: WizardContext, parentDisposable: Disposable): ModuleWizardStep {
        wizardStep = SpringInitializrWizardStep(context)
        StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_INITIALIZR_WIZARD)
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