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

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationInfo

object PluginContext {
    val pluginVersion by lazy {
        PluginManager.getPluginByClass(SentryErrorReporter::class.java)?.version ?: "Unknown"
    }

    val ideVersion by lazy {
        ApplicationInfo.getInstance().build.asStringWithoutProductCode()
    }

    val ideName: String by lazy {
        ApplicationInfo.getInstance().build.productCode
    }

    val ideBuild: String by lazy {
        ApplicationInfo.getInstance().build.toString()
    }
}