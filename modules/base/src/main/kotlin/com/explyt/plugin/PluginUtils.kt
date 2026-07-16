/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.plugin

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.lang.Language
import com.intellij.openapi.extensions.PluginId


enum class PluginIds(val pluginId: String) {
    EXPLYT("com.explyt.test"),
    HTTP_CLIENT_JB("com.jetbrains.restClient"),
    SPRING_JB("com.intellij.spring"),
    SPRING_DATA_JB("com.intellij.spring.data"),
    SPRING_WEB_JB("com.intellij.spring.mvc"),
    SPRING_DEBUGGER_JB("com.intellij.spring.debugger"),
    CDI_JB("com.intellij.cdi"), //Jakarta EE - DI (QUARKUS SEXPLYT Conflict)
    SPRING_BOOT_JB("com.intellij.spring.boot"),
    ;

    fun findEnabled() = PluginManager.getInstance().findEnabledPlugin(PluginId.getId(pluginId))

    fun isEnabled() = findEnabled() != null

    fun isNotEnabled() = !isEnabled()

    /**
     * The plugin is enabled and paid Ultimate functionality is unlocked.
     *
     * Since the unified IntelliJ IDEA distribution, JetBrains plugins like [SPRING_JB] and [SPRING_BOOT_JB]
     * are bundled and enabled even in the free mode, but their paid features (line markers, inspections, etc.)
     * are cut: content modules requiring `com.intellij.modules.ultimate` are not loaded.
     * The OSS Community build does not bundle them at all.
     */
    fun isEnabledWithUltimate() = isEnabled() && isUltimateEnabled()

    companion object {
        private val ULTIMATE_MODULE_ID = PluginId.getId("com.intellij.modules.ultimate")

        /**
         * Paid Ultimate mode: the `com.intellij.modules.ultimate` module exists and is not disabled.
         * In the free mode of the unified distribution this module is disabled;
         * in the OSS Community build it does not exist at all.
         */
        fun isUltimateEnabled(): Boolean =
            !PluginManagerCore.isDisabled(ULTIMATE_MODULE_ID)
                    && PluginManager.getInstance().findEnabledPlugin(ULTIMATE_MODULE_ID) != null
    }
}

enum class PluginSqlLanguage(val langId: String) {
    SQL("SQL"),
    JPAQL("JPAQL"),
    HQL("HQL"),
    SPRING_QL("SpringDataQL"),
    ;

    fun isEnabled() = Language.findLanguageByID(langId) != null
}