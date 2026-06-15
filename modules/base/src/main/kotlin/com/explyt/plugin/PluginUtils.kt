/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.plugin

import com.intellij.ide.plugins.PluginManager
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
}

enum class PluginSqlLanguage(val langId: String) {
    SQL("SQL"),
    JPAQL("JPAQL"),
    HQL("HQL"),
    SPRING_QL("SpringDataQL"),
    ;

    fun isEnabled() = Language.findLanguageByID(langId) != null
}