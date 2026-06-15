/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.properties.providers.SpringMetadataValueProvider
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticInsertHandler
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class SpringMetadataValueProviderReference(element: PsiElement) : PsiReferenceBase<PsiElement>(element, true),
    EmptyResolveMessageProvider {
    override fun getUnresolvedMessagePattern(): String {
        return SpringCoreBundle.message("explyt.spring.inspection.metadata.config.unresolved.provider", this.value)
    }

    override fun resolve(): PsiElement? {
        val valueProvider = getValueProvider()
        return if (valueProvider != null) element else null
    }

    override fun getVariants(): Array<LookupElementBuilder> {
        return SpringMetadataValueProvider.entries.map {
            LookupElementBuilder.create(it.value)
                .appendTailText(" (${it.description})", true)
                .withInsertHandler(StatisticInsertHandler(StatisticActionId.COMPLETION_SPRING_ADDITIONAL_METADATA))
        }.toTypedArray()
    }

    fun getValueProvider(): SpringMetadataValueProvider? {
        return SpringMetadataValueProvider.findByName(value)
    }

}