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

package com.explyt.spring.core.runconfiguration.edit

import com.explyt.spring.core.profile.SpringProfilesService
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.module.Module
import com.intellij.ui.TextFieldWithAutoCompletionListProvider

class ProfilesCompletionProvider : TextFieldWithAutoCompletionListProvider<String>(null) {
    var module: Module? = null

    override fun getLookupString(item: String): String = item

    override fun getItems(
        prefix: String?,
        cached: Boolean,
        parameters: CompletionParameters?
    ): Collection<String> {
        val module = module ?: return emptyList()

        val exclude = getProfilesToExclude(parameters)

        return SpringProfilesService.getInstance(module.project)
            .loadExistingProfiles(module)
            .filter { it !in exclude }
            .sortedWith(Comparator { item1, item2 ->
                compare(item1, item2)
            })
    }

    private fun getProfilesToExclude(parameters: CompletionParameters?): Set<String> {
        return parameters?.let {
            val text = parameters.editor.document.text
            val offset = parameters.offset
            val comma = text.lastIndexOf(',', offset - 1) + 1

            return (text.substring(0, comma) + text.substring(offset))
                .trim()
                .split(',').asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        } ?: setOf()
    }

    override fun getPrefix(text: String, offset: Int): String {
        val comma = text.lastIndexOf(',', offset - 1) + 1

        return text.substring(comma, offset)
    }

    override fun compare(item1: String?, item2: String?): Int {
        return when {
            item1 == item2 -> 0
            item1 == null || item2 == "default" -> 1
            item2 == null || item1 == "default" -> -1
            else -> item1.compareTo(item2)
        }
    }
}