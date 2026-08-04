/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.action.UastModelTrackerInvalidateAction
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project

class ExplytRunManagerListener(val project: Project) : RunManagerListener {

    override fun runConfigurationSelected(settings: RunnerAndConfigurationSettings?) {
        super.runConfigurationSelected(settings)
        updateProfilesFromConfiguration(settings)
    }

    override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) {
        super.runConfigurationChanged(settings)
        val configuration = settings.configuration
        if (configuration is SpringBootRunConfiguration) {
            StatisticService.getInstance().addActionUsage(StatisticActionId.RUN_CONFIGURATION_CHANGED)
            syncLinkedProjectName(configuration)
        }
        updateProfilesFromConfiguration(settings)
    }

    /**
     * Keeps `NativeProjectSettings.runConfigurationName` in sync with the actual run configuration name.
     * When the user renames a Spring Boot run configuration, any linked Explyt Spring project that points to
     * the same main class gets its stored name refreshed, otherwise `RunConfigurationExtractor`
     * would fail to locate it during sync.
     *
     * This callback may run on EDT and while run configurations are being loaded, so the cheap checks come first:
     * the main class is resolved only for a link that predates [NativeProjectSettings.qualifiedMainClassName] and
     * actually needs renaming. The update stays synchronous, because `RunConfigurationExtractor` may look the name
     * up as soon as this callback returns.
     */
    private fun syncLinkedProjectName(configuration: SpringBootRunConfiguration) {
        val nativeSettings = project.getService(NativeSettings::class.java) ?: return
        val newName = configuration.name
        val outdatedSettings = nativeSettings.linkedProjectsSettings
            .filter { it.runConfigurationName != newName }
        if (outdatedSettings.isEmpty()) return

        // Stable identity: the qualified main class name is stored when the project is linked, so no PSI is needed.
        val mainClassName = configuration.mainClassName
        val linkedByMainClassName = mainClassName
            ?.let { name -> outdatedSettings.singleOrNull { it.qualifiedMainClassName == name } }
        if (linkedByMainClassName != null) {
            linkedByMainClassName.runConfigurationName = newName
            return
        }

        // Projects linked before the main class name was stored can only be matched by the main-class file path,
        // which requires resolving PSI. Reaching this point means such a link exists and its name is stale, so the
        // resolution cost is now paid only in that case instead of on every configuration change.
        val legacySettings = outdatedSettings.filter { it.qualifiedMainClassName == null }
        if (legacySettings.isEmpty()) return

        val mainFilePath = runReadActionBlocking {
            configuration.mainClass?.containingFile?.virtualFile?.canonicalPath
        } ?: return
        val linked = nativeSettings.getLinkedProjectSettings(mainFilePath)
            ?.takeIf { it in legacySettings }
            ?: return
        linked.runConfigurationName = newName
    }

    override fun stateLoaded(runManager: RunManager, isFirstLoadState: Boolean) {
        super.stateLoaded(runManager, isFirstLoadState)
        ProfilesService.getInstance(project).updateFromConfiguration(runManager.selectedConfiguration)
    }

    private fun updateProfilesFromConfiguration(
        settings: RunnerAndConfigurationSettings?
    ) {
        val profilesService = ProfilesService.getInstance(project)
        val isChanged = profilesService.updateFromConfiguration(settings)

        if (isChanged) {
            ApplicationManager.getApplication().invokeLater {
                ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, SYSTEM_ID)
                UastModelTrackerInvalidateAction.invalidate(project)
            }
        }
    }

}