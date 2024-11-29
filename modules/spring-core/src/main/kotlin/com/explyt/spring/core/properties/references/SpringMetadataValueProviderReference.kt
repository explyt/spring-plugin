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