/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPostfixExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.UastBinaryExpressionWithTypeKind
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType

/**
 * The declaration an expression stands for once the local copies made of it are seen through.
 *
 * Kotlin code commonly copies a nullable property into a local `val` to smart-cast it -
 * `val stats = statsService ?: throw ...` or `val stats = statsService; if (stats == null) ...` - and then calls the
 * local. The local is the same object as the property, so a question about the property, such as "is this an injected
 * bean", must be answered for the local too.
 *
 * A local is an alias only when it can never hold anything else: a Kotlin `val`, or a Java local that is `final` or
 * never written after its initializer. Its initializer is read through what preserves the value:
 * - parentheses;
 * - `!!` and a cast;
 * - `requireNotNull`/`checkNotNull`/`Objects.requireNonNull`;
 * - an elvis whose right side leaves the scope - `throw`, `return`, `error(...)`, `TODO()` - because only then is
 *   the result guaranteed to be the left side.
 */
internal object LocalAliases {

    private const val MAX_HOPS = 4

    /** What [expression] reads, seen through the wrappers that keep its value and through local aliases. */
    fun originOf(expression: UExpression?): PsiElement? =
        (expression?.let(::unwrap) as? UResolvable)?.resolve()?.let(::originOf)

    /** What [element] stands for: itself, unless it is a local alias, which is followed a bounded number of hops. */
    fun originOf(element: PsiElement): PsiElement? {
        val visited = HashSet<PsiElement>()
        var current = element
        repeat(MAX_HOPS) {
            if (!visited.add(current)) return null
            val initializer = aliasInitializerOf(current) ?: return current
            current = (unwrap(initializer) as? UResolvable)?.resolve() ?: return null
        }
        return current.takeIf { aliasInitializerOf(it) == null }
    }

    private fun aliasInitializerOf(element: PsiElement): UExpression? {
        val variable = element.toUElement() as? ULocalVariable ?: return null
        if (!isNeverReassigned(variable)) return null
        return variable.uastInitializer
    }

    private fun isNeverReassigned(variable: ULocalVariable): Boolean {
        (variable.sourcePsi as? KtVariableDeclaration)?.let { return !it.isVar }
        val local = variable.javaPsi as? PsiLocalVariable ?: return false
        if (local.hasModifierProperty(PsiModifier.FINAL)) return true
        val scope = PsiTreeUtil.getParentOfType(local, PsiCodeBlock::class.java) ?: return false
        return ReferencesSearch.search(local, LocalSearchScope(scope)).findAll()
            .none { (it.element as? PsiExpression)?.let(PsiUtil::isAccessedForWriting) == true }
    }

    private fun unwrap(expression: UExpression): UExpression {
        val inner = when {
            expression is UParenthesizedExpression -> expression.expression
            expression is UBinaryExpressionWithType &&
                    expression.operationKind is UastBinaryExpressionWithTypeKind.TypeCast -> expression.operand

            expression is UPostfixExpression && expression.operator.text == NON_NULL_ASSERTION -> expression.operand
            else -> elvisLeftOf(expression) ?: passedThroughArgumentOf(expression)
        }
        return inner?.let(::unwrap) ?: expression
    }

    private fun elvisLeftOf(expression: UExpression): UExpression? {
        val elvis = expression.sourcePsi as? KtBinaryExpression ?: return null
        if (elvis.operationToken != KtTokens.ELVIS || !leavesTheScope(elvis.right)) return null
        return elvis.left?.toUElementOfType<UExpression>()
    }

    private fun leavesTheScope(expression: KtExpression?): Boolean =
        when (val right = expression?.let(KtPsiUtil::safeDeparenthesize)) {
            is KtThrowExpression, is KtReturnExpression -> true
            is KtCallExpression -> right.toUElementOfType<UCallExpression>()?.resolve()?.let(::isKotlinFailure) == true
            else -> false
        }

    private fun isKotlinFailure(method: PsiMethod): Boolean =
        method.name in KOTLIN_FAILURES && method.containingClass?.qualifiedName?.startsWith("kotlin.") == true

    private fun passedThroughArgumentOf(expression: UExpression): UExpression? {
        val call = expression as? UCallExpression ?: return null
        val method = call.resolve() ?: return null
        val owner = method.containingClass?.qualifiedName ?: return null
        val passesThrough = (method.name in KOTLIN_NON_NULL_CHECKS && owner.startsWith("kotlin."))
                || (method.name == JAVA_NON_NULL_CHECK && owner == JAVA_OBJECTS)
        return call.valueArguments.firstOrNull()?.takeIf { passesThrough }
    }

    private const val NON_NULL_ASSERTION = "!!"
    private val KOTLIN_FAILURES = setOf("error", "TODO")
    private val KOTLIN_NON_NULL_CHECKS = setOf("requireNotNull", "checkNotNull")
    private const val JAVA_NON_NULL_CHECK = "requireNonNull"
    private const val JAVA_OBJECTS = "java.util.Objects"
}
