/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationInfo

object PluginContext {
    val pluginVersion by lazy {
        PluginManager.getPluginByClass(SentryErrorReporter::class.java)?.version ?: "Unknown"
    }

    /** Middle segment of the version string, e.g. "31" from "253.31.58" */
    val pluginMajorVersion: String by lazy {
        pluginVersion.split('.').getOrElse(1) { "unknown" }
    }

    val ideName: String by lazy {
        ApplicationInfo.getInstance().build.productCode
    }

    val ideBuild: String by lazy {
        ApplicationInfo.getInstance().build.let { it.productCode + "-" + it.components[0] }
    }

    val ideFullVersion: String by lazy {
        ApplicationInfo.getInstance().build.toString()
    }
}