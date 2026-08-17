/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.ide.plugins.PluginDetailsService
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId

object PluginContext {
    /** Plugin id declared in `spring-bootstrap/src/main/resources/META-INF/plugin.xml`. */
    const val PLUGIN_ID = "com.explyt.spring"

    val pluginVersion by lazy {
        @Suppress("UnstableApiUsage")
        PluginDetailsService.getInstance()
            .findDetails(PluginId.getId(PLUGIN_ID))
            ?.version ?: "Unknown"
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