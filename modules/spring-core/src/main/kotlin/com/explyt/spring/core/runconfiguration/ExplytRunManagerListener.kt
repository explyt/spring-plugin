package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.action.UastModelTrackerInvalidateAction
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.service.ProfilesService
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
        updateProfilesFromConfiguration(settings)
    }

    override fun stateLoaded(runManager: RunManager, isFirstLoadState: Boolean) {
        super.stateLoaded(runManager, isFirstLoadState)
        ProfilesService.getInstance(project)
            .updateFromConfiguration(runManager.selectedConfiguration)
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