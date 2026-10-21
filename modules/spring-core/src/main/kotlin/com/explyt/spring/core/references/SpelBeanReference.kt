/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.references

import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult

/**
 * The bean named by `@beanName` inside a SpEL block, as in `@Value("#{@myProps.cron}")` (issue #44).
 *
 * Deliberately not [ExplytBeanReference]: that one is built for an injection point, where a name is only a hint on
 * top of a required type, so when the name matches nothing it falls back to every candidate it found by type
 * (`NativeSearchService.findActiveBeanDeclarations`). SpEL carries no type — the name is the entire query — and
 * that fallback would resolve a misspelled `#{@myPropz.cron}` to every bean in the module, which is worse than
 * not resolving it: navigation lands somewhere unrelated and the typo looks intentional.
 */
class SpelBeanReference(
    element: PsiElement,
    private val beanName: String,
    rangeInElement: TextRange
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference, HighlightedReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        beansNamed(element, beanName)
            .map { PsiElementResolveResult(it.psiMember) }
            .toTypedArray()

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        return SpringSearchServiceFacade.getInstance(element.project).getAllActiveBeans(module)
            .map {
                LookupElementBuilder.create(it.name)
                    .withIcon(AllIcons.Nodes.Class)
                    .withTypeText(it.psiClass.containingFile?.name)
            }
            .toTypedArray()
    }

    companion object {
        /**
         * The beans registered under [beanName], matched by name and nothing else.
         *
         * Shared with [SpelBeanMemberReference] so the bean and the member read from it are resolved against the
         * same set — otherwise the two halves of one expression could disagree about which bean is meant.
         */
        fun beansNamed(element: PsiElement, beanName: String): List<PsiBean> {
            val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
            return SpringSearchServiceFacade.getInstance(element.project)
                .getAllBeanByNames(module)[beanName]
                ?: emptyList()
        }
    }
}
