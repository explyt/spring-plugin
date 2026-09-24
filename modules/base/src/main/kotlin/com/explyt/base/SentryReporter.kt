/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.ide.plugins.PluginDetailsService
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.util.concurrency.AppExecutorUtil
import io.sentry.Breadcrumb
import io.sentry.IScope
import io.sentry.ScopeType
import io.sentry.Sentry
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object SentryReporter {

    private const val DSN = "https://e45ddc59e273e25c659729f194b8009f@sentry.explyt.ai/4"

    /** Matches Sentry's own `maxBreadcrumbs` default, so buffering cannot retain more than Sentry would keep. */
    private const val MAX_PENDING_BREADCRUMBS = 100

    @Volatile
    private var initialized = false
    private val lock = ReentrantLock()

    private val pendingBreadcrumbs = BoundedBuffer<Breadcrumb>(MAX_PENDING_BREADCRUMBS)
    private val initializationScheduler = AsyncInitializationScheduler(
        executor = AppExecutorUtil.getAppExecutorService(),
        initialize = ::ensureInitialized,
        flush = ::flushPendingBreadcrumbs,
        isInitialized = { initialized },
        hasPending = { !pendingBreadcrumbs.isEmpty() },
        discardPending = { pendingBreadcrumbs.drain() },
    )

    private fun ensureInitialized() {
        if (initialized) return

        lock.withLock {
            if (!initialized) {
                val properties = System.getProperties()

                Sentry.init { options ->
                    options.dsn = DSN
                    // Tie every event to a release so Sentry can show Suspect Commits.
                    // Must match the release created by the release CI (see release.yaml).
                    options.release = PluginContext.pluginVersion
                    options.isEnableUncaughtExceptionHandler = false
                    // Sentry drops the whole event when this callback throws, so sanitizing must fail closed.
                    options.setBeforeSend { event, _ ->
                        event.breadcrumbs?.let { breadcrumbs ->
                            event.breadcrumbs = ActionBreadcrumbSanitizer.sanitizeSafely(breadcrumbs, event.timestamp)
                        }
                        event
                    }
                }

                // GLOBAL for the same reason as breadcrumbs: the default scope type is ISOLATION, which Sentry 8
                // keeps in a ThreadLocal. Initialization no longer runs on the thread that submits a report, so
                // thread-scoped tags would only reach an event by being cloned when that thread forks its scopes.
                // The global scope is shared by reference and merged into every event regardless of the thread.
                Sentry.configureScope(ScopeType.GLOBAL) { scope ->
                    scope.setTag("os.name", properties.getProperty("os.name", "unknown"))
                    scope.setTag("os.version", properties.getProperty("os.version", "unknown"))

                    val applicationInfo = ApplicationInfo.getInstance()
                    val build = applicationInfo.build
                    scope.setTag("ide.name", applicationInfo.fullApplicationName)
                    scope.setTag("ide.type", build.productCode)
                    scope.setTag("ide.version", build.baselineVersion.toString())
                    scope.setTag("ide.build", build.asString())

                    scope.setTag("plugin.version", PluginContext.pluginVersion)
                    scope.setTag("plugin.version.major", PluginContext.pluginMajorVersion)

                    scope.setTag("java.version", properties.getProperty("java.version", "unknown"))
                    scope.setTag("java.vendor", properties.getProperty("java.vendor", "unknown"))

                    scope.setTag(
                        "idea.kotlin.plugin.use.k2",
                        properties.getProperty("idea.kotlin.plugin.use.k2", "false")
                    )

                    scope.setContexts("Non-Bundled Plugins", collectNonBundledPlugins())
                }

                initialized = true
            }
        }
    }

    /**
     * Third-party plugins reported as Sentry context.
     *
     * [PluginDetailsService.PluginDetails.isBuiltIn] covers both bundled plugins and updates of bundled ones,
     * which is slightly broader than the previous `isBundled` check: an updated bundled plugin is no longer
     * reported. The platform exposes no public API distinguishing the two, and the internal one fails
     * plugin verification, so the broader filter is accepted here.
     */
    @Suppress("UnstableApiUsage")
    private fun collectNonBundledPlugins(): Map<String, String> =
        PluginDetailsService.getInstance().getActivePlugins()
            .filter { !it.isBuiltIn }
            .associate { it.id.idString to (it.version ?: "unknown") }

    /**
     * Records an action breadcrumb without ever initializing Sentry on the calling thread.
     *
     * This is called from `AnActionListener.afterActionPerformed`, i.e. on the EDT after *every* action. Sentry's
     * initialization is not cheap — [collectNonBundledPlugins] reads the manifest of every active plugin — so paying
     * for it here froze the UI on the first action after IDE start. Until Sentry is up, breadcrumbs are buffered and
     * initialization is handed to a pooled thread; a burst of actions schedules it exactly once.
     *
     * Telemetry is best-effort: dropping the oldest breadcrumbs is preferable to delaying an action.
     */
    fun addBreadcrumb(breadcrumb: Breadcrumb) {
        if (initialized) {
            flushPendingBreadcrumbs()
            recordBreadcrumb(breadcrumb)
            return
        }

        pendingBreadcrumbs.offer(breadcrumb)
        initializationScheduler.schedule()
    }

    /**
     * Also called from the fast path above, because a breadcrumb can be buffered by another thread just after the
     * flush that follows initialization; without this it would stay in the buffer until the next report.
     */
    private fun flushPendingBreadcrumbs() {
        if (pendingBreadcrumbs.isEmpty()) return
        pendingBreadcrumbs.drain().forEach { recordBreadcrumb(it) }
    }

    /**
     * Breadcrumbs go to the **global** scope on purpose.
     *
     * Sentry 8 stores current and isolation scopes in a `ThreadLocal`, and `Sentry.addBreadcrumb` writes to the
     * isolation scope of whichever thread calls it. Initialization now happens on a pooled thread, so anything
     * written to a thread-scoped view would be invisible to the background thread that later submits the report.
     * `CombinedScopeView.getBreadcrumbs` merges the global scope into every event, which makes the breadcrumb trail
     * independent of the thread that recorded it.
     */
    private fun recordBreadcrumb(breadcrumb: Breadcrumb) {
        Sentry.configureScope(ScopeType.GLOBAL) { scope -> scope.addBreadcrumb(breadcrumb) }
    }

    /**
     * Configures the Sentry scope with an `error_report` tag and scope configuration logic.
     */
    fun withErrorReportTag(configureScope: (IScope) -> Unit) {
        // Already invoked from a background task, so initializing here is safe; it also covers the case of a report
        // submitted before any action had a chance to schedule initialization.
        ensureInitialized()
        flushPendingBreadcrumbs()
        Sentry.withScope { scope ->
            scope.setTag("kind", "error_report")
            configureScope(scope)
        }
    }
}
