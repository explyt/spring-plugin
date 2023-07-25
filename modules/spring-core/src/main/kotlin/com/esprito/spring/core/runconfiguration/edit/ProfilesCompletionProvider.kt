package com.esprito.spring.core.runconfiguration.edit

import com.esprito.spring.core.profile.SpringProfilesService
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
        val exclude = prefix
            ?.split(",")
            ?.map { it.trim() }
            ?: listOf()

        val module = module ?: return emptyList()
        return SpringProfilesService.getInstance(module.project)
            .loadExistingProfiles(module)
            .filter { it !in exclude }
    }

    override fun getPrefix(text: String, offset: Int): String {
        val comma = text.lastIndexOf(',', offset - 1) + 1
        return text.substring(comma, offset)
    }
}