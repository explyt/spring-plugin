/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner

/**
 * The annotations a Spring proxy acts on around a method: transactions, asynchronous execution and caching.
 *
 * Spring reads them from the method and from its declaring class, so both are reported, the method's own first. A
 * project annotation carrying one of them as a meta-annotation - a `@ReadOnlyTransaction` shortcut - is reported as
 * the annotation it stands for, since that is the behaviour a caller has to reason about.
 */
internal object ProxyAnnotations {

    enum class DeclaredOn { METHOD, CLASS }

    data class Declared(val annotation: String, val declaredOn: DeclaredOn)

    private val PROXY_ANNOTATIONS = SpringCoreClasses.AOP_ANNOTATION + JavaEeClasses.TRANSACTIONAL.allFqns

    fun of(method: PsiMethod): List<Declared> =
        declaredOn(method, DeclaredOn.METHOD) + method.containingClass?.let { declaredOn(it, DeclaredOn.CLASS) }.orEmpty()

    private fun declaredOn(owner: PsiModifierListOwner, declaredOn: DeclaredOn): List<Declared> =
        owner.annotations.flatMap { annotation ->
            PROXY_ANNOTATIONS.filter { annotation.isMetaAnnotatedByOrSelf(it) }.map { Declared(it, declaredOn) }
        }.distinct()
}
