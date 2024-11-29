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

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.action.UastModelTrackerInvalidateAction
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project

class ExplytRunManagerListener(val project: Project) : RunManagerListener {

    override fun runConfigurationSelected(settings: RunnerAndConfigurationSettings?) {
        super.runConfigurationSelected(settings)
        updateProfilesFromConfiguration(settings)
    }

    override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) {
        super.runConfigurationChanged(settings)
        if (settings.configuration is SpringBootRunConfiguration) {
            StatisticService.getInstance().addActionUsage(StatisticActionId.RUN_CONFIGURATION_CHANGED)
        }
        updateProfilesFromConfiguration(settings)
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