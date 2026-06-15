/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.language.profiles

import com.explyt.spring.core.profile.SpringProfilesService
import com.explyt.spring.core.statistic.StatisticActionId.COMPLETION_PROFILES
import com.explyt.spring.core.statistic.StatisticInsertHandler
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
            .map {
                LookupElementBuilder
                    .create(it)
                    .withInsertHandler(StatisticInsertHandler(COMPLETION_PROFILES))
            }
            .forEach { result.addElement(it) }
    }

}