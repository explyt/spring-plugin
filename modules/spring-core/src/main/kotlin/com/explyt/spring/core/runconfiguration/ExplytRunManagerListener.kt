/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.action.UastModelTrackerInvalidateAction
import com.explyt.spring.core.externalsystem.NativeLinkRepairService
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
     * [runConfigurationChanged] only sees renames made in this IDE. Run configurations shared through `.run` files
     * use the configuration name as their scheme key, so a rename renames the file too: pulling that change deletes
     * one configuration and adds another, and no `changed` event is ever published. Repairing on add — which also
     * fires for every configuration while the project opens — is what heals links broken on another machine.
     */
    override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
        super.runConfigurationAdded(settings)
        NativeLinkRepairService.getInstance(project).scheduleRepair()
    }

    /**
     * Schedules a repair, deliberately keeping the now-stale stored name: that name is what marks the link as
     * repairable. Clearing it would make the link invisible to the repair pass and downgrade it to an *unbound* link
     * (a `null` name means "discover by path or by the selected configuration"), so `RunConfigurationExtractor` would
     * fabricate a default configuration instead of reporting the broken link.
     */
    override fun runConfigurationRemoved(settings: RunnerAndConfigurationSettings) {
        super.runConfigurationRemoved(settings)
        NativeLinkRepairService.getInstance(project).scheduleRepair()
    }

    /**
     * Keeps `NativeProjectSettings.runConfigurationName` in sync with the actual run configuration name.
     * When the user renames a Spring Boot run configuration, any linked Explyt Spring project that points to
     * the same main class gets its stored name refreshed, otherwise `RunConfigurationExtractor`
     * would fail to locate it during sync.
     *
     * This callback may run on EDT and while run configurations are being loaded, so it only compares stored names:
     * a link matched by [NativeProjectSettings.qualifiedMainClassName] is renamed synchronously, because
     * `RunConfigurationExtractor` may look the name up as soon as this callback returns.
     *
     * Every other link — a legacy link without a stored class name, or a Kotlin link whose stored
     * `@SpringBootApplication` class (`...Foo`) never equals the configuration's file facade (`...FooKt`) — can only be
     * matched through the main-class file, which needs PSI and indexes. That match is left to
     * [NativeLinkRepairService], which resolves it in smart mode off the EDT and renames only dangling stored names
     * with a unique candidate.
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

        NativeLinkRepairService.getInstance(project).scheduleRepair()
    }

    override fun stateLoaded(runManager: RunManager, isFirstLoadState: Boolean) {
        super.stateLoaded(runManager, isFirstLoadState)
        ProfilesService.getInstance(project).updateFromConfiguration(runManager.selectedConfiguration)
        // A fresh clone carries linked projects but no local event history, so project open is the only chance to
        // heal a link whose stored name was renamed away on another machine.
        NativeLinkRepairService.getInstance(project).scheduleRepair()
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
