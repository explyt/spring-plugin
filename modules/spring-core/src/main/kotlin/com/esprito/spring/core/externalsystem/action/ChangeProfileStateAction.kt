package com.esprito.spring.core.externalsystem.action

import com.esprito.spring.core.externalsystem.model.SpringProfileData
import com.esprito.spring.core.runconfiguration.RunConfigurationUtil
import com.esprito.spring.core.runconfiguration.SpringBootRunConfiguration
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
        ApplicationManager.getApplication().runWriteAction {
            val configurationName = profileData.configurationName
            val springRunConfiguration = RunManager.getInstance(project).allConfigurationsList
                .find { it.name == configurationName } as? SpringBootRunConfiguration ?: return@runWriteAction
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