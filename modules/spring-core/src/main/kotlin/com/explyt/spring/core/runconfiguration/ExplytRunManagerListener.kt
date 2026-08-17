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
     * Drops a stored name that just disappeared, keeping `externalProjectPath` and `qualifiedMainClassName` so the
     * link can still be re-bound by main-class file once the replacing configuration arrives.
     */
    override fun runConfigurationRemoved(settings: RunnerAndConfigurationSettings) {
        super.runConfigurationRemoved(settings)
        val nativeSettings = project.getService(NativeSettings::class.java) ?: return
        nativeSettings.linkedProjectsSettings
            .filter { it.runConfigurationName == settings.name }
            .forEach { it.runConfigurationName = null }
        NativeLinkRepairService.getInstance(project).scheduleRepair()
    }

    /**
     * Keeps `NativeProjectSettings.runConfigurationName` in sync with the actual run configuration name.
     * When the user renames a Spring Boot run configuration, any linked Explyt Spring project that points to
     * the same main class gets its stored name refreshed, otherwise `RunConfigurationExtractor`
     * would fail to locate it during sync.
     *
     * This callback may run on EDT and while run configurations are being loaded, so the cheap checks come first:
     * the main class is resolved only for a link whose stored name is dangling and actually needs renaming.
     * The update stays synchronous, because `RunConfigurationExtractor` may look the name up as soon as this
     * callback returns.
     *
     * Kotlin needs the file-path fallback even for links that do store [NativeProjectSettings.qualifiedMainClassName]:
     * for a top-level `main()` the configuration's main class is the file facade (`...FooKt`), while the link stores
     * the `@SpringBootApplication` class (`...Foo`), so those two identities can never match. The main-class *file*
     * is what both sides agree on. Renaming is then guarded by [isDanglingName]: several configurations may share one
     * main class, and a stored name that still belongs to a live configuration must survive a sibling's change.
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

        // Links that cannot be matched by the stored class name are matched by the main-class file path, which
        // requires resolving PSI. Only a dangling stored name is repaired this way, so the resolution cost is paid
        // in that case alone instead of on every configuration change.
        val danglingSettings = outdatedSettings.filter { isDanglingName(it.runConfigurationName) }
        if (danglingSettings.isEmpty()) return

        val mainFilePath = runReadActionBlocking {
            configuration.mainClass?.containingFile?.virtualFile?.canonicalPath
        } ?: return
        val linked = nativeSettings.getLinkedProjectSettings(mainFilePath)
            ?.takeIf { it in danglingSettings }
            ?: return
        linked.runConfigurationName = newName
    }

    /** A stored name is dangling when no run configuration bears it any more, so claiming it cannot steal a live link. */
    private fun isDanglingName(storedName: String?): Boolean {
        storedName ?: return true
        return RunManager.getInstance(project).allSettings.none { it.name == storedName }
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