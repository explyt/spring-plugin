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

import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult

open class MetaConfigurationKeyReference(
    element: PsiElement,
    protected val module: Module,
    private val propertyKey: String,
    textRange: TextRange? = null
) : PsiReferenceBase.Poly<PsiElement>(element, textRange, false) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val metadataProperties = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getElementNameProperties(module)
        var result: List<JsonProperty> = metadataProperties.asSequence()
            .filter { it.name == propertyKey }
            .mapTo(mutableListOf()) { it.jsonProperty }

        result = getOnlySourceIfExist(result)
        return PsiElementResolveResult.createResults(result)
    }

    private fun getOnlySourceIfExist(result: List<JsonProperty>): List<JsonProperty> {
        if (result.size <= 1) {
            return result
        }

        val resultWithSources = mutableListOf<JsonProperty>()
        for (it in result) {
            val path = it.containingFile.containingDirectory.virtualFile.canonicalPath
            if (path != null && path.contains("sources")) {
                resultWithSources.add(it)
            }
        }
        if (resultWithSources.isNotEmpty()) {
            return resultWithSources
        }

        return result
    }

}