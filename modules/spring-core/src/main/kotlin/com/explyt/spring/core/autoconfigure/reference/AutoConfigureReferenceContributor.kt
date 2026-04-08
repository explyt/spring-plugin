/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.autoconfigure.reference

import com.explyt.plugin.PluginIds
import com.explyt.spring.core.SpringProperties.PROPERTY_VALUE_DELIMITERS
import com.explyt.spring.core.autoconfigure.language.AutoConfigurationImportsFileType
import com.explyt.spring.core.autoconfigure.language.FactoriesFileType
import com.explyt.spring.core.providers.JavaSoftAllowDollarClassReferenceProvider
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.openapi.util.text.DelimitedListProcessor
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext

class AutoConfigureReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        if (PluginIds.SPRING_BOOT_JB.isEnabled()) return

        val filePattern = PlatformPatterns.virtualFile().ofType(FactoriesFileType)
        val autoConfigureImportsFilePattern = PlatformPatterns.virtualFile().ofType(AutoConfigurationImportsFileType)

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyKeyImpl::class.java).inVirtualFile(filePattern),
            FactoriesPsiReferenceKeyProvider(),
            PsiReferenceRegistrar.DEFAULT_PRIORITY
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyKeyImpl::class.java).inVirtualFile(autoConfigureImportsFilePattern),
            FactoriesPsiReferenceKeyProvider(),
            PsiReferenceRegistrar.DEFAULT_PRIORITY
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyValueImpl::class.java).inVirtualFile(filePattern),
            AutoConfigureReferenceProvider,
            PsiReferenceRegistrar.DEFAULT_PRIORITY
        )
    }
}

private class FactoriesPsiReferenceKeyProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val text = element.text.trim()
        if (text.contains(" ")) return PsiReference.EMPTY_ARRAY
        return JavaSoftAllowDollarClassReferenceProvider.getReferencesByString(text, element, 0)
    }
}

private object AutoConfigureReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val text = element.text
        var result = PsiReference.EMPTY_ARRAY
        object : DelimitedListProcessor(PROPERTY_VALUE_DELIMITERS) {
            override fun processToken(start: Int, end: Int, delimitersOnly: Boolean) {
                val substringText = text.substring(start, end)
                val psiReferences =
                    JavaSoftAllowDollarClassReferenceProvider.getReferencesByString(substringText, element, start)
                result = ArrayUtil.mergeArrays(result, psiReferences)
            }
        }.processText(text)
        return result
    }
}

