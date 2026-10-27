/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.JavaCoreClasses
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiWildcardType
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * A request handler's signature as its declaration states it, rather than as the JVM sees it.
 *
 * A Kotlin `suspend` function compiles to a method with one more, last parameter - the coroutine continuation
 * `Continuation<? super T>` - and the return type `Object`. The framework supplies the continuation itself and answers
 * with the `T` the function declares, so the compiled shape reports a parameter that is not part of the request and a
 * response of type `Object`.
 */
object HandlerSignature {

    /** The parameters a request binds, without the continuation of a `suspend` function. */
    fun requestParameters(method: PsiMethod): List<PsiParameter> {
        val parameters = method.parameterList.parameters.asList()
        return if (continuationOf(method) != null) parameters.dropLast(1) else parameters
    }

    /**
     * The type the handler answers with: the `T` a `suspend` function declares, `void` for `Unit` as for a
     * non-suspend Kotlin function, and the method's own return type otherwise.
     */
    fun declaredReturnType(method: PsiMethod): PsiType? {
        val continuation = continuationOf(method) ?: return method.returnType
        val answered = (continuation.type as? PsiClassType)?.parameters?.singleOrNull() ?: return method.returnType
        val declared = (answered as? PsiWildcardType)?.bound ?: answered
        return if (declared.equalsToText(KOTLIN_UNIT)) PsiTypes.voidType() else declared
    }

    private fun continuationOf(method: PsiMethod): PsiParameter? {
        val last = method.parameterList.parameters.lastOrNull() ?: return null
        val erased = (last.type as? PsiClassType)?.rawType()?.canonicalText
        if (erased != JavaCoreClasses.KOTLIN_CONTINUATION) return null
        return last.takeIf { isSuspend(method) }
    }

    private fun isSuspend(method: PsiMethod): Boolean {
        val function = (method as? KtLightMethod)?.kotlinOrigin ?: method.navigationElement
        return (function as? KtNamedFunction)?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true
    }

    private const val KOTLIN_UNIT = "kotlin.Unit"
}
