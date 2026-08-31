/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.completion.properties.MetadataDeclarations
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
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
        val declarations = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getElementNameProperties(module)
            .filter { it.name == propertyKey }
        // The same key is declared by both metadata files of an artifact and again in its sources jar; they are one
        // declaration, so only the preferred copy of each artifact is offered.
        val result = MetadataDeclarations
            .distinct(declarations, { it.name }, { it.jsonProperty.containingFile })
            .map { it.jsonProperty }
        return PsiElementResolveResult.createResults(result)
    }
}
