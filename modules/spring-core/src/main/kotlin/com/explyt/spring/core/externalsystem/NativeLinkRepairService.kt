/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem

import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Re-binds Explyt Spring project links whose stored run configuration name no longer exists.
 *
 * [com.explyt.spring.core.runconfiguration.ExplytRunManagerListener.runConfigurationChanged] only covers renames
 * performed in this IDE. For run configurations shared through `.run` files the scheme key *is* the configuration
 * name, so renaming one renames its file: a teammate pulling that commit observes a file deletion plus a file
 * addition, i.e. `runConfigurationRemoved` followed by `runConfigurationAdded`, and `runConfigurationChanged` never
 * fires. A fresh clone has no local event history at all. Repairing on add and on project open is therefore the only
 * way to heal a link that was broken on somebody else's machine.
 *
 * A link is repaired only when exactly one run configuration points at its main-class file: configurations sharing a
 * main class differ in profiles, VM arguments and environment, so an ambiguous match is left alone rather than
 * guessed, consistently with [RunConfigurationExtractor].
 */
@Service(Service.Level.PROJECT)
class NativeLinkRepairService(private val project: Project) : Disposable {

    private val repairScheduled = AtomicBoolean(false)
    private val repairRequestedAgain = AtomicBoolean(false)

    /**
     * Schedules a coalesced repair pass off the EDT.
     *
     * The callers are load-path listeners that can fire while `RunManager` is still initializing — `stateLoaded` is
     * published from inside that initialization — and touching `RunManager.getInstance` from there re-enters the
     * service container and fails with a cycle. The whole pass, including the cheap check, is therefore deferred
     * until the current initialization has finished; a burst of load events collapses into a single pass.
     *
     * Events arriving while a pass is in flight are not dropped: they raise [repairRequestedAgain], which triggers
     * exactly one follow-up pass, so a configuration removed or added mid-pass is still observed.
     */
    fun scheduleRepair() {
        if (!repairScheduled.compareAndSet(false, true)) {
            repairRequestedAgain.set(true)
            return
        }
        ApplicationManager.getApplication()
            .invokeLater({ startRepair() }, ModalityState.nonModal(), project.disposed)
    }

    /** Ends a pass and runs at most one follow-up for the events that arrived while it was in flight. */
    private fun finishPass() {
        repairScheduled.set(false)
        if (repairRequestedAgain.compareAndSet(true, false)) {
            scheduleRepair()
        }
    }

    /**
     * Runs the dangling-link check in memory first: nothing is submitted unless a link actually needs repairing, so
     * a healthy project pays only a walk over the linked settings and never resolves PSI.
     */
    private fun startRepair() {
        if (findDanglingSettings().isEmpty()) {
            finishPass()
            return
        }

        ReadAction.nonBlocking(Callable { computeRepairs() })
            .expireWith(this)
            .coalesceBy(this)
            // Resolving a main class goes through JavaPsiFacade and the indexes, which are unavailable in dumb mode.
            // Project open — the case this pass exists for — is exactly when indexing runs, and a failure here would
            // waste the only scheduled attempt, so wait for smart mode instead.
            .inSmartMode(project)
            .finishOnUiThread(ModalityState.nonModal()) { applyRepairs(it) }
            .submit(AppExecutorUtil.getAppExecutorService())
            // onProcessed also covers cancellation and expiration, so the flag never stays stuck.
            .onProcessed { finishPass() }
    }

    /** Synchronous variant for tests, which must not race the background pipeline. */
    @VisibleForTesting
    fun repairNow() {
        if (findDanglingSettings().isEmpty()) return
        applyRepairs(runReadActionBlocking { computeRepairs() })
    }

    /**
     * Collects the links worth repairing. The `-DebugSession-` link is not a real project link: it is keyed by a
     * transient debug session, never by a run configuration name, so it is skipped.
     */
    private fun findDanglingSettings(): List<NativeProjectSettings> {
        val nativeSettings = project.getService(NativeSettings::class.java) ?: return emptyList()
        val runManager = RunManager.getInstance(project)
        return nativeSettings.linkedProjectsSettings.filter { settings ->
            settings.externalProjectPath != Constants.DEBUG_SESSION_NAME && isDangling(settings, runManager)
        }
    }

    /**
     * A `null` stored name is **not** dangling: a link created without a run configuration uses it to mean "discover
     * by main-class path or by the selected configuration", and `RunConfigurationExtractor` depends on that state.
     * Only a name that was stored and no longer resolves marks a link as broken.
     */
    private fun isDangling(settings: NativeProjectSettings, runManager: RunManager): Boolean {
        val storedName = settings.runConfigurationName ?: return false
        return runManager.allSettings.none { it.name == storedName }
    }

    private fun computeRepairs(): List<Repair> {
        val danglingSettings = findDanglingSettings()
        if (danglingSettings.isEmpty()) return emptyList()

        val configurationsByMainFilePath = mutableMapOf<String, MutableList<RunConfiguration>>()
        for (configuration in RunManager.getInstance(project).allConfigurationsList) {
            ProgressManager.checkCanceled()
            val mainFilePath = mainFilePath(configuration) ?: continue
            configurationsByMainFilePath.getOrPut(mainFilePath) { mutableListOf() } += configuration
        }

        return danglingSettings.mapNotNull { settings ->
            ProgressManager.checkCanceled()
            // Fail closed on ambiguity: the profiles/environment of the wrong sibling would silently be used.
            val configuration = configurationsByMainFilePath[settings.externalProjectPath]?.singleOrNull()
            configuration?.let { Repair(settings, it.name) }
        }
    }

    /**
     * The main-class *file* is the identity a run configuration and a link agree on: for a Kotlin top-level `main()`
     * the configuration holds the file facade (`...FooKt`) while the link stores the `@SpringBootApplication` class
     * (`...Foo`), so their qualified names never match.
     */
    private fun mainFilePath(configuration: RunConfiguration): String? =
        NativeBootUtils.getMainClass(configuration)?.containingFile?.virtualFile?.canonicalPath

    /**
     * Runs on the EDT after a background computation, so the world may have moved on: a configuration can have been
     * removed or the link repaired meanwhile. Each repair is revalidated before it is written, otherwise a name that
     * no longer exists could be stored back into the settings.
     */
    private fun applyRepairs(repairs: List<Repair>) {
        if (repairs.isEmpty()) return
        val runManager = RunManager.getInstance(project)
        repairs.asSequence()
            .filter { isDangling(it.settings, runManager) }
            .filter { repair -> runManager.allSettings.any { it.name == repair.runConfigurationName } }
            .forEach { it.settings.runConfigurationName = it.runConfigurationName }
    }

    override fun dispose() = Unit

    private data class Repair(val settings: NativeProjectSettings, val runConfigurationName: String)

    companion object {
        fun getInstance(project: Project): NativeLinkRepairService = project.service()
    }
}
