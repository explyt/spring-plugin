/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.action.UastModelTrackerInvalidateAction
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
     * the same main-class file gets its stored name refreshed, otherwise `RunConfigurationExtractor`
     * would fail to locate it during sync.
     */
    private fun syncLinkedProjectName(configuration: SpringBootRunConfiguration) {
        val mainFilePath = runReadActionBlocking {
            configuration.mainClass?.containingFile?.virtualFile?.canonicalPath
        } ?: return
        val nativeSettings = project.getService(NativeSettings::class.java) ?: return
        val linked = nativeSettings.getLinkedProjectSettings(mainFilePath) ?: return
        if (linked.runConfigurationName != configuration.name) {
            linked.runConfigurationName = configuration.name
        }
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