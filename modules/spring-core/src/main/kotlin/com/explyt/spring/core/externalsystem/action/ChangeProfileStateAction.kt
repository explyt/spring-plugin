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

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.externalsystem.model.SpringProfileData
import com.explyt.spring.core.runconfiguration.RunConfigurationUtil
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.execution.RunManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project

class ChangeProfileStateAction : ExternalSystemNodeAction<SpringProfileData>(SpringProfileData::class.java) {

    override fun perform(
        project: Project,
        projectSystemId: ProjectSystemId,
        profileData: SpringProfileData,
        e: AnActionEvent
    ) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_CHANGE_PROFILE)
        ApplicationManager.getApplication().runWriteAction {
            val configurationName = profileData.configurationName
            val springRunConfiguration = RunManager.getInstance(project).allConfigurationsList
                .filterIsInstance<SpringBootRunConfiguration>()
                .find { it.name == configurationName } ?: return@runWriteAction
            val profileSet = RunConfigurationUtil.stringToProfile(springRunConfiguration.springProfiles)
            val profilesString = getUpdatedProfilesString(profileSet, profileData)
            springRunConfiguration.springProfiles = profilesString
        }
        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, projectSystemId)
    }

    private fun getUpdatedProfilesString(profileSet: Set<String>, profileData: SpringProfileData): String {
        return if (profileSet.contains(profileData.name)) {
            profileSet.filter { it != profileData.name }.joinToString(",")
        } else {
            (profileSet + profileData.name).joinToString(",")
        }
    }
}