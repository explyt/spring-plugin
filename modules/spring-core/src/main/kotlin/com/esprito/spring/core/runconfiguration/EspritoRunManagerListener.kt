package com.esprito.spring.core.runconfiguration

import com.esprito.spring.core.service.ProfilesService
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project

class EspritoRunManagerListener(val project: Project) : RunManagerListener {

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
        val oldProfiles = profilesService.activeProfiles
        val newProfiles = profilesService
            .updateFromConfiguration(settings)

        if (oldProfiles != newProfiles) {
            ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker().incModificationCount()
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

}