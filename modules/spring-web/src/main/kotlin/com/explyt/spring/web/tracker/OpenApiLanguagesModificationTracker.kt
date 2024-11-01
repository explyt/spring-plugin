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

package com.explyt.spring.web.tracker

import com.intellij.json.JsonLanguage
import com.intellij.json.json5.Json5Language
import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.YAMLLanguage

@Service(Service.Level.PROJECT)
class OpenApiLanguagesModificationTracker(val project: Project) : ModificationTracker {

    private var languageTrackers: List<ModificationTracker>

    init {
        val psiManager = PsiManager.getInstance(project)
        val languages = listOf<Language>(JsonLanguage.INSTANCE, Json5Language.INSTANCE, YAMLLanguage.INSTANCE)
        languageTrackers = languages.map { psiManager.modificationTracker.forLanguage(it) }
    }

    override fun getModificationCount(): Long {
        return languageTrackers.sumOf { it.modificationCount }
    }
}