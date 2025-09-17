/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.data.providers

import com.explyt.spring.data.SpringDataClasses
import com.explyt.spring.data.SpringDataClasses.JDBC_CLIENT_STATEMENT
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.Unmodifiable
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val JDBC_CLIENT_METHOD_PARAM = "param"

class JdbcClientReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val injection = injectionHostUExpression()
        val referenceProvider = JdbcClientReferenceProvider()
        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(
                0,
                PsiJavaPatterns.psiMethod()
                    .withName(JDBC_CLIENT_METHOD_PARAM)
                    .definedInClass(JDBC_CLIENT_STATEMENT)
                    .withParameterCount(2),
                true
            ),
            referenceProvider,
            PsiReferenceRegistrar.LOWER_PRIORITY
        )
        registrar.registerUastReferenceProvider(
            injection.methodCallParameter(
                0,
                PsiJavaPatterns.psiMethod()
                    .withName(JDBC_CLIENT_METHOD_PARAM)
                    .definedInClass(JDBC_CLIENT_STATEMENT)
                    .withParameterCount(3),
                true
            ),
            referenceProvider,
            PsiReferenceRegistrar.LOWER_PRIORITY
        )
    }
}

private class JdbcClientReferenceProvider() : UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val sourcePsi = uExpression.sourcePsi ?: return emptyArray()
        val uParamMethodExpression = uExpression.uastParent?.uastParent as? UExpression ?: return emptyArray()
        val queryMethodVisitor = PsiSqlQueryMethodVisitor()
        uParamMethodExpression.accept(queryMethodVisitor)
        val sqlUExpression = queryMethodVisitor.uExpression ?: return emptyArray()
        return listOf(getReference(sourcePsi, uExpression, sqlUExpression)).toTypedArray()
    }

    private fun getReference(
        sourcePsi: PsiElement,
        uExpression: UExpression,
        sqlUExpression: UExpression
    ): PsiReference {
        return if (uExpression.isNumberLiteral())
            SqlIndexedParamReference(sourcePsi, uExpression, sqlUExpression)
        else
            SqlNamedParamReference(sourcePsi, uExpression, sqlUExpression)
    }

}

private class PsiSqlQueryMethodVisitor() : AbstractUastVisitor() {
    var uExpression: UExpression? = null

    override fun visitCallExpression(node: UCallExpression): Boolean {
        val uSqlExpression = findSqlExpression(node)
        return if (uSqlExpression != null) {
            uExpression = uSqlExpression
            return true
        } else {
            super.visitCallExpression(node)
        }
    }
}

class SqlNamedParamReference(
    sourcePsi: PsiElement,
    private val uSourceExpression: UExpression,
    private val uSqlExpression: UExpression,
) : PsiReferenceBase.Poly<PsiElement>(sourcePsi, ElementManipulators.getValueTextRange(sourcePsi), true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val paramName = uSourceExpression.evaluateString() ?: return emptyArray()
        val results = getTargetPsi(uSqlExpression, ":$paramName")
        return results.map { PsiElementResolveResult(it) }.toTypedArray()
    }
}

class SqlIndexedParamReference(
    sourcePsi: PsiElement,
    private val uSourceExpression: UExpression,
    private val uSqlExpression: UExpression,
) : PsiReferenceBase<PsiElement>(sourcePsi, ElementManipulators.getValueTextRange(sourcePsi), true) {

    override fun resolve(): PsiElement? {
        val paramIdx = uSourceExpression.evaluateString()?.toInt()?.minus(1) ?: return null
        val results = getTargetPsi(uSqlExpression, "?")
        return try {
            results[paramIdx]
        } catch (_: Exception) {
            null
        }
    }
}

fun findSqlExpression(node: UCallExpression): UExpression? {
    if (node.kind != UastCallKind.METHOD_CALL) return null
    node.methodName?.takeIf { it == "sql" } ?: return null
    val psiMethod = node.resolve() ?: return null
    val targetClass = psiMethod.containingClass ?: return null
    if (targetClass.qualifiedName != SpringDataClasses.JDBC_CLIENT) return null
    val uExpression = node.getArgumentForParameter(0) ?: return null
    return (uExpression.tryResolve()?.toUElement() as? UVariable)?.uastInitializer ?: uExpression
}

fun getTargetPsi(uExpression: UExpression, stringToFind: String): List<PsiElement> {
    return if (uExpression is UPolyadicExpression) {
        uExpression.operands.flatMap { getTargetPsiFromExpression(it, stringToFind) }
    } else {
        getTargetPsiFromExpression(uExpression, stringToFind)
    }
}

private fun getTargetPsiFromExpression(uExpression: UExpression, stringToFind: String): List<PsiElement> {
    val sourcePsi = uExpression.sourcePsi ?: return emptyList()
    val findInjectedElementAt = getInjectedPsiData(sourcePsi)?.firstOrNull()?.first ?: return emptyList()
    val results = mutableListOf<PsiElement>()
    findInjectedElementAt.accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element.text == stringToFind) {
                results.add(element)
            }
            super.visitElement(element)
        }
    })
    return results.distinctBy { it.textRange }
}

private fun getInjectedPsiData(sourcePsi: PsiElement): @Unmodifiable List<Pair<PsiElement?, TextRange?>?>? {
    val injectedLanguageManager = InjectedLanguageManager.getInstance(sourcePsi.project)
    return injectedLanguageManager.getInjectedPsiFiles(sourcePsi)
        ?: sourcePsi.parent?.let { injectedLanguageManager.getInjectedPsiFiles(it) }
}
