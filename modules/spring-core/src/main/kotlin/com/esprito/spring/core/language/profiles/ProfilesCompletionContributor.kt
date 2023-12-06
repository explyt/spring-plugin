package com.esprito.spring.core.language.profiles

import com.esprito.spring.core.profile.SpringProfilesService
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.modules
import org.apache.commons.lang3.StringUtils

class ProfilesCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        val profilesService = SpringProfilesService.getInstance(position.project)

        position.project.modules.asSequence()
            .flatMap { profilesService.loadExistingProfiles(it).asSequence() }
            .map { StringUtils.trim(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .map { LookupElementBuilder.create(it) }
            .forEach { result.addElement(it) }
    }

}