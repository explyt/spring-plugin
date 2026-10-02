/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.USuperExpression
import org.jetbrains.uast.UThisExpression

/**
 * One place where a method body hands control to another method: a call `bean.validate(input)`, or a method passed
 * as a callable reference `input.use(bean::validate)`, which the receiving function invokes.
 *
 * Both shapes reach the same method on the same receiver, so the call trace and the endpoint contract read them
 * through this one value instead of a branch per shape.
 *
 * @property callee the method reached.
 * @property receiver the instance the method is invoked on, or `null` when there is none: an unqualified call, or a
 *   reference qualified by a type, such as `Repo::count` or `String::trim`, whose instance is supplied later by the
 *   function the reference is passed to.
 * @property receiverClass the class of [receiver], or of the qualifying type of a type-qualified reference.
 * @property line the 1-based line of the method's name at the call site.
 */
internal class MethodCallSite private constructor(
    val callee: PsiMethod,
    val receiver: UExpression?,
    val receiverClass: PsiClass?,
    val line: Int?,
) {

    /** Whether the method is invoked on the caller itself (`this::helper`, `super.x()`) rather than on another object. */
    val isOnSelf: Boolean get() = receiver is UThisExpression || receiver is USuperExpression

    companion object {

        fun of(call: UCallExpression): MethodCallSite? {
            val callee = call.resolve() ?: return null
            val line = (call.methodIdentifier?.sourcePsi ?: call.sourcePsi)?.let(McpSourcePositions::lineOfAnchor)
            return MethodCallSite(callee, call.receiver, PsiUtil.resolveClassInClassTypeOnly(call.receiverType), line)
        }

        /**
         * A reference to a method, or `null` for a reference to a property or a field: reading a value is not a call,
         * although a Kotlin property reference such as `Order::label` resolves to the property's getter. A constructor
         * reference (`::Order`) reaches the constructor, which the trace leaves out exactly as it does a constructor
         * call.
         *
         * Kotlin reports the class of a type qualifier both as the qualifier expression, which then resolves to a
         * class, and as `qualifierType`; Java reports the qualifier expression only. A qualifier that resolves to a
         * class is therefore a type, not an instance.
         */
        fun of(reference: UCallableReferenceExpression): MethodCallSite? {
            val callee = (reference.resolve() as? PsiMethod)?.takeUnless(::isPropertyAccessor) ?: return null
            val qualifier = reference.qualifierExpression
            val qualifiedByType = (qualifier as? UResolvable)?.resolve() is PsiClass
            val receiver = qualifier.takeUnless { qualifiedByType }
            val receiverClass = PsiUtil.resolveClassInClassTypeOnly(receiver?.getExpressionType() ?: reference.qualifierType)
                ?: (qualifier as? UResolvable)?.resolve() as? PsiClass
            val line = reference.sourcePsi?.let(McpSourcePositions::lineOfAnchor)
            return MethodCallSite(callee, receiver, receiverClass, line)
        }

        private fun isPropertyAccessor(method: PsiMethod): Boolean {
            val origin = (method as? KtLightMethod)?.kotlinOrigin
            return origin is KtProperty || origin is KtPropertyAccessor || origin is KtParameter
        }
    }
}
