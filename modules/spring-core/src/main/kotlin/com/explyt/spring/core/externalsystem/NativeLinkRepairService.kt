/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem

import com.explyt.spring.core.externalsystem.setting.NativeExecutionSettings
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
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.ThreadingAssertions
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

private val logger = logger<NativeLinkRepairService>()

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
     * Schedules a coalesced repair pass on a pooled thread.
     *
     * The callers are load-path listeners that can fire while `RunManager` is still initializing — `stateLoaded` is
     * published from inside that initialization — so touching `RunManager.getInstance` inline re-enters the service
     * container and fails with a cycle. The pass is therefore deferred, and a burst of load events collapses into a
     * single pass.
     *
     * It must not be deferred *to the EDT*, which is what the pass originally did: requesting a project service that
     * has not been created yet instantiates it synchronously, and the requesting thread is parked for the whole
     * initialization. On the EDT that is a UI freeze, reported for `RunManager` in issue #294. A pooled thread waits
     * for exactly the same initialization without blocking the UI.
     *
     * Events arriving while a pass is in flight are not dropped: they raise [repairRequestedAgain], which triggers
     * exactly one follow-up pass, so a configuration removed or added mid-pass is still observed.
     */
    fun scheduleRepair() {
        if (!repairScheduled.compareAndSet(false, true)) {
            repairRequestedAgain.set(true)
            return
        }
        AppExecutorUtil.getAppExecutorService().execute { startRepair() }
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
     *
     * The check deliberately runs *before* — and outside of — the read action below, because it is what forces the
     * `RunManager` service to be created: doing that while holding the read lock would park this thread inside
     * service initialization with the lock taken, stalling every write action queued behind it.
     */
    private fun startRepair() {
        ThreadingAssertions.assertBackgroundThread()
        var passHandedOver = false
        try {
            if (project.isDisposed || findDanglingSettings().isEmpty()) return

            ReadAction.nonBlocking(Callable { computeRepairs() })
                .expireWith(this)
                .coalesceBy(this)
                // Resolving a main class goes through JavaPsiFacade and the indexes, which are unavailable in dumb
                // mode. Project open — the case this pass exists for — is exactly when indexing runs, and a failure
                // here would waste the only scheduled attempt, so wait for smart mode instead.
                .inSmartMode(project)
                .finishOnUiThread(ModalityState.nonModal()) { applyRepairs(it) }
                .submit(AppExecutorUtil.getAppExecutorService())
                // onProcessed also covers cancellation and expiration, so the flag never stays stuck.
                .onProcessed { finishPass() }
            passHandedOver = true
        } finally {
            // A pooled pass can outlive the project that scheduled it and fail on an already-disposed service; the
            // flag must be cleared on every exit, otherwise self-healing stays off for the rest of the session.
            if (!passHandedOver) finishPass()
        }
    }

    /** Synchronous variant for tests, which must not race the background pipeline. */
    @VisibleForTesting
    fun repairNow() {
        if (findDanglingSettings().isEmpty()) return
        applyRepairs(runReadActionBlocking { computeRepairs() })
    }

    /**
     * Handles a link whose stored run configuration name no longer exists, discovered mid-sync by
     * [com.explyt.spring.core.externalsystem.SpringBeanNativeResolver]: the extractor deliberately returns nothing
     * for a dangling name instead of fabricating a configuration, so without intervention every refresh fails the
     * same way for a project that may be invisible in the tool window.
     *
     * Heal-first: when exactly one run configuration points at the link's main-class file, [executionSettings] is
     * rebound to it and re-resolved through [reresolve]. The stored settings are updated only when that
     * re-resolution succeeds — a candidate of a configuration type this link cannot launch would leave the stored
     * name unresolvable. A rebinding that fails re-resolution is rolled back, so the sync error keeps naming the
     * real stored name.
     *
     * Without a candidate, a link that has no import data is removed: it produces a sync error on every refresh and
     * shows nothing in return. A link with import data keeps failing loudly instead — its stale tree node is visible
     * and may still be wanted. Links whose stored name still resolves are out of scope here: a launch or build
     * failure of an existing configuration is a transient application problem, not a broken link.
     *
     * Settings mutations are posted to the EDT, as in the rest of the link lifecycle, and revalidated there: a link
     * healed or re-linked while this pass was in flight must not be removed. On the EDT itself (tests) they run
     * inline, keeping the call synchronous.
     *
     * @return `true` when the link was healed and [executionSettings] carries the candidate's name
     */
    fun healOrPruneDanglingLink(
        projectPath: String,
        executionSettings: NativeExecutionSettings,
        reresolve: () -> Boolean,
    ): Boolean {
        val storedName = executionSettings.runConfigurationName ?: return false
        // The debug-session link is keyed by a transient session, never by a run configuration name.
        if (projectPath == Constants.DEBUG_SESSION_NAME) return false
        val runManager = RunManager.getInstance(project)
        if (!isDanglingName(storedName, runManager)) return false
        val candidates = runReadActionBlocking { findConfigurationsByMainFile(runManager, projectPath) }
        // Several configurations on the same main-class file: guessing one would silently take another's profiles
        // and environment, and deleting the link would destroy it — leave it untouched and failing loudly.
        if (candidates.size > 1) return false
        val candidate = candidates.singleOrNull()
        if (candidate != null) {
            executionSettings.runConfigurationName = candidate.name
            if (reresolve()) {
                logger.info("Explyt link repair: healed a dangling link during sync")
                logger.debug { "Explyt link repair: rebind $storedName -> ${candidate.name} for $projectPath" }
                persistHealedName(projectPath, storedName, candidate.name)
                return true
            }
            executionSettings.runConfigurationName = storedName
            logger.info("Explyt link repair: the healing candidate does not resolve for this link, keeping the stored name")
            return false
        }
        if (hasImportData(projectPath)) return false
        logger.info("Explyt link repair: pruning a dangling link that has no import data")
        logger.debug { "Explyt link repair: pruning $storedName at $projectPath" }
        pruneLink(projectPath, storedName)
        return false
    }

    /** The tool window renders a project only from its imported structure, so stored data means the link is visible. */
    private fun hasImportData(projectPath: String): Boolean =
        ExternalSystemApiUtil.findProjectNode(project, Constants.SYSTEM_ID, projectPath)?.data != null

    /**
     * Written on the EDT like the other settings mutations in this class, and only after revalidation: the healing
     * candidate was computed on a pooled thread, and the world may have moved on since.
     */
    private fun persistHealedName(projectPath: String, previousName: String, healedName: String) {
        runOnEdt {
            val linked = project.getService(NativeSettings::class.java).getLinkedProjectSettings(projectPath)
                ?.takeIf { it.externalProjectPath == projectPath } ?: return@runOnEdt
            if (linked.runConfigurationName != previousName) return@runOnEdt
            if (RunManager.getInstance(project).allSettings.none { it.name == healedName }) return@runOnEdt
            linked.runConfigurationName = healedName
        }
    }

    private fun pruneLink(projectPath: String, storedName: String) {
        runOnEdt {
            val linked = project.getService(NativeSettings::class.java).getLinkedProjectSettings(projectPath)
                ?.takeIf { it.externalProjectPath == projectPath } ?: return@runOnEdt
            if (linked.runConfigurationName != storedName) return@runOnEdt
            if (!isDanglingName(storedName, RunManager.getInstance(project))) return@runOnEdt
            if (hasImportData(projectPath)) return@runOnEdt
            ExternalSystemApiUtil.getSettings(project, Constants.SYSTEM_ID).unlinkExternalProject(projectPath)
        }
    }

    /** Posts to the EDT from the resolver's pooled thread; runs inline on the EDT, which keeps tests synchronous. */
    private fun runOnEdt(action: () -> Unit) {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) action() else application.invokeLater(action, project.disposed)
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

    private fun isDangling(settings: NativeProjectSettings, runManager: RunManager): Boolean =
        isDanglingName(settings.runConfigurationName, runManager)

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

        /**
         * A `null` stored name is **not** dangling: a link created without a run configuration uses it to mean
         * "discover by main-class path or by the selected configuration", and `RunConfigurationExtractor` depends on
         * that state. Only a name that was stored and no longer resolves marks a link as broken.
         */
        internal fun isDanglingName(storedName: String?, runManager: RunManager): Boolean {
            storedName ?: return false
            return runManager.allSettings.none { it.name == storedName }
        }

        /**
         * The main-class *file* is the identity a run configuration and a link agree on: for a Kotlin top-level
         * `main()` the configuration holds the file facade (`...FooKt`) while the link stores the
         * `@SpringBootApplication` class (`...Foo`), so their qualified names never match.
         */
        internal fun mainFilePath(configuration: RunConfiguration): String? =
            NativeBootUtils.getMainClass(configuration)?.containingFile?.virtualFile?.canonicalPath

        /** All configurations pointing at the link's main-class file; the caller decides how many it can handle. */
        internal fun findConfigurationsByMainFile(runManager: RunManager, mainFilePath: String): List<RunConfiguration> =
            runManager.allConfigurationsList.filter { mainFilePath(it) == mainFilePath }
    }
}
