/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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