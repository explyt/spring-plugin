/*
 * Copyright © 2025 Explyt Ltd
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