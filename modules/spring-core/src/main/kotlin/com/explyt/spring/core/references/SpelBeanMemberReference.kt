/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.references

import com.explyt.util.ExplytPsiUtil
import com.explyt.util.ExplytPsiUtil.isPublic
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PropertyUtilBase

/**
 * The member read by a SpEL property access on a bean reference — the `cron` of `@Scheduled(cron = "#{@myProps.cron}")`.
 *
 * The shape is idiomatic, not a mistake: `ScheduledAnnotationBeanPostProcessor` passes the attribute through
 * `EmbeddedValueResolver.resolveStringValue`, which evaluates SpEL after placeholder resolution, so a bean-backed
 * cron expression works at runtime. It therefore deserves the same navigation a `${...}` placeholder key already
 * gets (issue #44).
 *
 * Resolution follows SpEL's own property accessor order: the getter first, then a public field, and finally a
 * no-argument method of that name, which covers the `#{@bean.compute}` form written without parentheses.
 */
class SpelBeanMemberReference(
    element: PsiElement,
    private val beanName: String,
    private val memberName: String,
    rangeInElement: TextRange
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference, HighlightedReference {

    /**
     * Poly-variant because a bean name can be contributed by more than one declaration in a module — the same
     * class registered by both a `@Component` scan and a `@Bean` method, or two profile-specific definitions.
     * Resolving to one of them arbitrarily would send navigation to whichever the index happened to list first.
     */
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> = beanClasses()
        .mapNotNull { findMember(it, memberName) }
        .distinct()
        .map { PsiElementResolveResult(it) }
        .toTypedArray()

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> = beanClasses().asSequence()
        .flatMap { PropertyUtilBase.getAllProperties(it, false, true).keys.asSequence() }
        .distinct()
        .map { LookupElementBuilder.create(it).withIcon(AllIcons.Nodes.Property) }
        .toList().toTypedArray()

    /**
     * The classes behind [beanName], looked up by name alone through the same helper the bean half of the
     * expression uses, so both references agree on which bean is meant.
     */
    private fun beanClasses(): List<PsiClass> =
        SpelBeanReference.beansNamed(element, beanName).map { it.psiClass }

    private fun findMember(psiClass: PsiClass, name: String): PsiMember? =
        PropertyUtilBase.findPropertyGetter(psiClass, name, false, true)
            ?: psiClass.findFieldByName(name, true)?.takeIf { it.isPublic }
            ?: psiClass.allMethods.firstOrNull { it.name == name && ExplytPsiUtil.fitsForReference(it) }
}
