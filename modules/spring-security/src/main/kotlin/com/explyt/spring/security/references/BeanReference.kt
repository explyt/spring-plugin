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

package com.explyt.spring.security.references

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.security.SpringIcons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult

class BeanReference(
    element: PsiElement,
    range: TextRange,
    private val beanName: String
) : PsiReferenceBase.Poly<PsiElement>(element, range, false) {
    override fun resolve(): PsiElement? {
        val resolveResults: Array<out ResolveResult> = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val springSearchService = SpringSearchServiceFacade.getInstance(element.project)
        val beanPsiClass = springSearchService.getAllBeanByNames(module).asSequence()
            .filter { it.key == beanName }
            .flatMap { it.value.map { psiBean -> psiBean.psiMember } }
            .toList()

        return beanPsiClass.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val allBeans = SpringSearchServiceFacade.getInstance(myElement.project).getAllActiveBeans(module)
        return allBeans.map { bean ->
            LookupElementBuilder.create(bean.name)
                .withIcon(SpringIcons.SpringBean)
                .withTypeText(bean.psiClass.containingFile?.name)
        }.toTypedArray()
    }

}