/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytAnnotationUtil.getMemberValues
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.UastEmptyExpression
import org.jetbrains.uast.toUElement

object ActuatorMediaTypes {
    private const val PRODUCES_FROM = "producesFrom"
    private const val PRODUCIBLE_METHOD = "getProducedMimeType"
    private const val MIME_TYPE_VALUE_OF = "valueOf"
    private const val VOID = "void"
    private const val KOTLIN_UNIT = "kotlin.Unit"
    private const val OCTET_STREAM = "application/octet-stream"

    private val defaults = listOf(
        "application/vnd.spring-boot.actuator.v3+json",
        "application/vnd.spring-boot.actuator.v2+json",
        "application/json",
    )

    fun producedBy(method: PsiMethod, annotation: PsiAnnotation?): List<String> {
        val declared = annotation?.getMemberValues("produces")
            ?.map { it.text.trim('"') }
            .orEmpty()
        if (declared.isNotEmpty()) return declared + producedFrom(annotation)

        val from = producedFrom(annotation)

        if (from.isNotEmpty()) return from

        val returnType = method.returnType ?: return defaults
        if (returnType.canonicalText in setOf(VOID, KOTLIN_UNIT)) return emptyList()
        val classType = returnType as? PsiClassType
        if (classType?.isResource() == true || classType?.isResourceResponse() == true) {
            return listOf(OCTET_STREAM)
        }
        return defaults
    }

    private fun producedFrom(annotation: PsiAnnotation?): List<String> =
        annotation?.getMemberValues(PRODUCES_FROM).orEmpty()
            .mapNotNull { value ->
                ((value.toUElement() as? UClassLiteralExpression)?.type as? PsiClassType)?.resolve()
                    ?: value.references.asSequence().mapNotNull { it.resolve() as? PsiClass }.firstOrNull()
                    ?: value.text.substringBefore("::").trim().substringAfterLast('.').let { shortName ->
                        JavaPsiFacade.getInstance(value.project).findClass(
                            shortName, GlobalSearchScope.allScope(value.project)
                        )
                    }
            }
            .filter { it.qualifiedName != SpringCoreClasses.ACTUATOR_PRODUCIBLE }
            .flatMap { it.producibleMimeTypes() }

    private fun PsiClass.producibleMimeTypes(): List<String> {

        val method = findMethodsByName(PRODUCIBLE_METHOD, false).firstOrNull() ?: return emptyList()
        return fields.filterIsInstance<PsiEnumConstant>().mapNotNull { constant ->
            val bodyValue = (method.toUElement() as? org.jetbrains.uast.UMethod)?.uastBody
                ?.let { evaluateMimeType(it, constant) }
            bodyValue ?: constant.argumentList?.expressions?.firstOrNull()?.text
                ?.trim()?.takeIf { it.startsWith("\"") && it.endsWith("\"") }
                ?.removeSurrounding("\"")
                ?: Regex("\"([^\"]+)\"").find(constant.text)?.groupValues?.get(1)
        }
    }

    private fun evaluateMimeType(expression: UExpression, constant: PsiEnumConstant): String? = when (expression) {
        is UBlockExpression -> expression.expressions.firstNotNullOfOrNull { evaluateMimeType(it, constant) }
        is UReturnExpression -> expression.returnExpression?.let { evaluateMimeType(it, constant) }
        is UCallExpression -> if (expression.methodName == MIME_TYPE_VALUE_OF) {
            expression.valueArguments.singleOrNull()?.let { evaluateValue(it, constant) }
        } else null
        is UastEmptyExpression -> null
        else -> expression.evaluate() as? String
    }

    private fun evaluateValue(expression: UExpression, constant: PsiEnumConstant): String? {
        val value = expression.evaluate() as? String
        if (value != null) return value
        val reference = expression as? UReferenceExpression ?: return null
        val parameter = reference.resolve() as? PsiNamedElement ?: return null
        val index = containingEnumConstructor(constant)?.parameterList?.parameters
            ?.indexOfFirst { it.name == parameter.name } ?: return null
        val argument = constant.argumentList?.expressions?.getOrNull(index) ?: return null
        return (argument.toUElement() as? UExpression)?.evaluate() as? String
            ?: argument.text.trim().takeIf { it.startsWith("\"") && it.endsWith("\"") }
                ?.removeSurrounding("\"")
    }

    private fun containingEnumConstructor(constant: PsiEnumConstant): PsiMethod? =
        constant.containingClass?.constructors?.firstOrNull { constructor ->
            constructor.parameterList.parameters.size == constant.argumentList?.expressionCount
        }

    private fun PsiClassType.isResource(): Boolean =
        resolve()?.qualifiedName == SpringCoreClasses.IO_RESOURCE

    private fun PsiClassType.isResourceResponse(): Boolean =
        resolve()?.qualifiedName == SpringCoreClasses.ACTUATOR_WEB_ENDPOINT_RESPONSE &&
            parameters.singleOrNull()?.let { it is PsiClassType && it.isResource() } == true
}
