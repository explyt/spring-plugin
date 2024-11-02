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

import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PropertyUtil

abstract class ConfigurationPropertiesDocumentationProvider<T : PsiElement> : JavaDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val psiElement = (element as? ConfigKeyPsiElement)?.parent ?: return null
        val extractedOriginalElement = extractOriginalElement(originalElement) ?: return null
        val generateDoc = super.generateDoc(psiElement, originalElement) ?: return null

        val sectionsIdx = generateDoc.indexOf(DocumentationMarkup.SECTIONS_START)
        if (sectionsIdx == -1 || generateDoc.contains(DocumentationMarkup.CONTENT_START)) {
            return generateDoc
        }

        val propertyDescription = getPropertyDescription(psiElement, extractedOriginalElement)
        if (propertyDescription.isNullOrEmpty()) {
            return generateDoc
        }

        return """${generateDoc.substring(0, sectionsIdx)}
${DocumentationMarkup.CONTENT_START}
<p>$propertyDescription</p>
${DocumentationMarkup.CONTENT_END}
${generateDoc.substring(sectionsIdx + DocumentationMarkup.SECTIONS_START.length)}"""
    }

    private fun getPropertyDescription(psiElement: PsiElement, extractedOriginalElement: T): String? {
        if (psiElement !is PsiMethod) {
            return null
        }

        val propertiesFile = extractedOriginalElement.containingFile
        if (!SpringCoreUtil.isConfigurationPropertyFile(propertiesFile)) {
            return null
        }

        if (!PropertyUtil.isSetterName(psiElement.name)) {
            return null
        }

        val module = ModuleUtilCore.findModuleForPsiElement(extractedOriginalElement) ?: return null
        val propertyFullKey = getPropertyFullKey(extractedOriginalElement)
        val configurationProperty = SpringConfigurationPropertiesSearch.getInstance(extractedOriginalElement.project)
            .findProperty(module, propertyFullKey) ?: return null
        return configurationProperty.description
    }

    abstract fun getPropertyFullKey(extractedOriginalElement: T): String

    protected abstract fun extractOriginalElement(originalElement: PsiElement?): T?
}
