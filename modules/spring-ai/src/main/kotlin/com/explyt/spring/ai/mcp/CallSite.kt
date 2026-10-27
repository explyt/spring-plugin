/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression

/**
 * One place where a method body hands control to another method, resolved once: a [MethodCallSite] when the IDE
 * knows the callee, an [UnresolvedCallSite] when it does not, `null` when the expression is not a call at all - a
 * reference to a property or a field.
 */
internal sealed interface CallSite {

    /** The 1-based line of the call site. */
    val line: Int?

    companion object {

        fun of(call: UCallExpression): CallSite? =
            when (val callee = call.resolve()) {
                null -> UnresolvedCallSite.of(call)
                else -> MethodCallSite.resolvedTo(call, callee)
            }

        fun of(reference: UCallableReferenceExpression): CallSite? =
            when (val callee = reference.resolve()) {
                null -> UnresolvedCallSite.of(reference)
                is PsiMethod -> MethodCallSite.resolvedTo(reference, callee)
                else -> null
            }
    }
}
