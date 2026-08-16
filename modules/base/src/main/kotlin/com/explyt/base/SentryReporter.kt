/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.ide.plugins.PluginDetailsService
import com.intellij.openapi.application.ApplicationInfo
import io.sentry.Breadcrumb
import io.sentry.IScope
import io.sentry.Sentry
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object SentryReporter {

    private const val DSN = "https://e45ddc59e273e25c659729f194b8009f@sentry.explyt.ai/4"

    @Volatile
    private var initialized = false
    private val lock = ReentrantLock()

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
                }

                Sentry.configureScope { scope ->
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

    fun addBreadcrumb(breadcrumb: Breadcrumb) {
        ensureInitialized()
        Sentry.addBreadcrumb(breadcrumb)
    }

    /**
     * Configures the Sentry scope with an `error_report` tag and scope configuration logic.
     */
    fun withErrorReportTag(configureScope: (IScope) -> Unit) {
        ensureInitialized()
        Sentry.withScope { scope ->
            scope.setTag("kind", "error_report")
            configureScope(scope)
        }
    }
}
