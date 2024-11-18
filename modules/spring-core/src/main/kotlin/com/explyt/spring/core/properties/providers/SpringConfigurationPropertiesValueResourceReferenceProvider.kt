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

package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.completion.properties.ProviderHint
import com.explyt.spring.core.references.PrefixReference
import com.explyt.spring.core.references.PrefixReferenceType
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.propertyKey
import com.explyt.spring.core.util.PropertyUtil.propertyValue
import com.explyt.spring.core.util.PropertyUtil.propertyValuePsiElement
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.intellij.util.text.findTextRange

class SpringConfigurationPropertiesValueResourceReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val propertyKey = element.propertyKey() ?: return emptyArray()
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return emptyArray()
        }
        return getResourceReferences(element, propertyKey)
    }

    private fun getResourceReferences(element: PsiElement, propertyKey: String): Array<PsiReference> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val propertyHint = PropertyUtil.getPropertyHint(module, propertyKey) ?: return emptyArray()

        return propertyHint.providers.asSequence()
            .flatMap { processProviderHints(element, it).asSequence() }
            .toList().toTypedArray()
    }

    private fun processProviderHints(element: PsiElement, provider: ProviderHint): Array<PsiReference> {
        val targetClassFqn = provider.parameters?.target ?: return emptyArray()
        val name = provider.name ?: return emptyArray()
        if (name == SpringProperties.HANDLE_AS && targetClassFqn == SpringCoreClasses.IO_RESOURCE) {
            return getResourceVariants(element)
        }
        return emptyArray()
    }

    private fun getResourceVariants(element: PsiElement): Array<PsiReference> {
        val text = element.propertyValue()?.substringBefore(DUMMY_IDENTIFIER_TRIMMED) ?: ""
        val valueElement = element.propertyValuePsiElement() ?: return emptyArray()
        val textRange = element.text.findTextRange(valueElement.text) ?: return emptyArray()
        val references = mutableListOf<PsiReference>()
        when {
            text.startsWith(SpringProperties.PREFIX_HTTP) ->
                references += PropertyUtil.getReferenceWithoutPrefix(text, element, textRange, emptyArray(), this)

            text.startsWith(SpringProperties.PREFIX_FILE) ->
                references += PropertyUtil.getReferenceByFilePrefix(text, element, textRange, emptyArray(), this)

            text.startsWith(SpringProperties.PREFIX_CLASSPATH) ->
                references += PropertyUtil.getReferenceByClasspathPrefix(
                    text,
                    SpringProperties.PREFIX_CLASSPATH,
                    element,
                    textRange,
                    emptyArray(),
                    this
                )

            text.startsWith(SpringProperties.PREFIX_CLASSPATH_STAR) ->
                references += PropertyUtil.getReferenceByClasspathPrefix(
                    text,
                    SpringProperties.PREFIX_CLASSPATH_STAR,
                    element,
                    textRange,
                    emptyArray(),
                    this
                )

            else -> {
                references += PrefixReference(element, textRange, PrefixReferenceType.FILE_PROPERTY)
                references += PropertyUtil.getReferenceWithoutPrefix(text, element, textRange, emptyArray(), this)
            }
        }

        return references.toTypedArray()
    }
}
