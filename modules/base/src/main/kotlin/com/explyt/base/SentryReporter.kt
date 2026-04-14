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

package com.explyt.base

import com.intellij.openapi.application.ApplicationInfo
import io.sentry.IScope
import io.sentry.Sentry
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object SentryReporter {

    private const val DSN = "https://a99462e81c8011f19d2f56e10d7aebdf@sentry.explyt.ai/2"

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
                }

                initialized = true
            }
        }
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
