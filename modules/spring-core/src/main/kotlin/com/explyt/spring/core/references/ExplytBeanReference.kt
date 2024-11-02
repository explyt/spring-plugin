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

package com.explyt.spring.core.references

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

class ExplytBeanReference(
    element: PsiElement,
    private val beanName: String,
    rangeInElement: TextRange
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference, HighlightedReference {

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val springSearchService = SpringSearchServiceFacade.getInstance(element.project)
        val foundBeanDeclarations = springSearchService.findActiveBeanDeclarations(module, beanName, element.language)
        return foundBeanDeclarations.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val resolveResults: Array<out ResolveResult> = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        val project: Project = myElement.project
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val allBeans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        return allBeans.map { bean ->
            LookupElementBuilder.create(bean.name)
                .withIcon(AllIcons.Nodes.Class)
                .withTypeText(bean.psiClass.containingFile?.name)
        }.toTypedArray()
    }
}