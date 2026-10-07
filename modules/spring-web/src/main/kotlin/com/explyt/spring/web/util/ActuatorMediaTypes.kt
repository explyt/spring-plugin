/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytAnnotationUtil.computeConstantExpression
import com.explyt.util.ExplytAnnotationUtil.getMemberValues
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UEnumConstant
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElement

object ActuatorMediaTypes {
    private const val PRODUCES = "produces"
    private const val PRODUCES_FROM = "producesFrom"
    private const val PRODUCIBLE_METHOD = "getProducedMimeType"
    private const val VOID = "void"
    private const val KOTLIN_UNIT = "kotlin.Unit"
    private const val OCTET_STREAM = "application/octet-stream"
    private const val MAX_TRACE_DEPTH = 4

    private val mimeTypeFactories = setOf("valueOf", "parseMediaType")
    private val mimeTypeClasses = setOf(SpringCoreClasses.MIME_TYPE, SpringWebClasses.MEDIA_TYPE)

    private val defaults = listOf(
        "application/vnd.spring-boot.actuator.v3+json",
        "application/vnd.spring-boot.actuator.v2+json",
        "application/json",
    )

    fun producedBy(method: PsiMethod, annotation: PsiAnnotation?): List<String> {
        val declared = annotation.getMemberValues(PRODUCES).mapNotNull { it.stringValue() } + producedFrom(annotation)
        if (declared.isNotEmpty()) return declared

        val returnType = method.returnType ?: return defaults
        if (returnType.canonicalText in setOf(VOID, KOTLIN_UNIT)) return emptyList()
        val classType = returnType as? PsiClassType ?: return defaults
        val returnClass = classType.resolve()?.qualifiedName
        if (returnClass == SpringCoreClasses.JAVA_LANG_VOID) return emptyList()
        if (returnClass == SpringCoreClasses.IO_RESOURCE || classType.isResourceResponse()) return listOf(OCTET_STREAM)
        return defaults
    }

    private fun PsiAnnotationMemberValue.stringValue(): String? =
        computeConstantExpression() as? String ?: (toUElement() as? UExpression)?.evaluate() as? String

    private fun producedFrom(annotation: PsiAnnotation?): List<String> =
        annotation.getMemberValues(PRODUCES_FROM)
            .mapNotNull { it.producibleClass() }
            .filter { it.qualifiedName != SpringCoreClasses.ACTUATOR_PRODUCIBLE }
            .flatMap { it.producibleMimeTypes() }

    private fun PsiAnnotationMemberValue.producibleClass(): PsiClass? =
        ((toUElement() as? UClassLiteralExpression)?.type as? PsiClassType)?.resolve()
            ?: references.firstNotNullOfOrNull { it.resolve() as? PsiClass }
            ?: JavaPsiFacade.getInstance(project).findClass(
                text.substringBefore("::").trim().substringAfterLast('.'), GlobalSearchScope.allScope(project)
            )

    private fun PsiClass.producibleMimeTypes(): List<String> {
        val method = findMethodsByName(PRODUCIBLE_METHOD, false).singleOrNull() ?: return emptyList()
        val returned = returnedExpression((method.toUElement() as? UMethod)?.uastBody) ?: return emptyList()
        return fields.filterIsInstance<PsiEnumConstant>()
            .filter { it.initializingClass == null }
            .mapNotNull { mimeTypeOf(returned, it, 0) }
    }

    private fun returnedExpression(body: UExpression?): UExpression? = when (body) {
        is UBlockExpression -> (body.expressions.lastOrNull() as? UReturnExpression)
            ?.takeIf { body.expressions.count { it is UReturnExpression } == 1 }
            ?.returnExpression
        is UReturnExpression -> body.returnExpression
        else -> body
    }

    private fun mimeTypeOf(expression: UExpression?, constant: PsiEnumConstant, depth: Int): String? {
        if (depth > MAX_TRACE_DEPTH) return null
        return when (val value = expression?.skipParenthesizedExprDown()) {
            is UQualifiedReferenceExpression -> (value.selector as? UCallExpression)?.let { mimeTypeCall(it, constant, depth) }
            is UCallExpression -> mimeTypeCall(value, constant, depth)
            is UReferenceExpression -> mimeTypeOf(fieldAssignment(value.resolve()), constant, depth + 1)
            else -> null
        }
    }

    private fun mimeTypeCall(call: UCallExpression, constant: PsiEnumConstant, depth: Int): String? {
        if (call.methodName !in mimeTypeFactories) return null
        if (call.resolve()?.containingClass?.qualifiedName !in mimeTypeClasses) return null
        return stringOf(call.valueArguments.singleOrNull(), constant, depth + 1)
    }

    private fun stringOf(expression: UExpression?, constant: PsiEnumConstant, depth: Int): String? {
        if (expression == null || depth > MAX_TRACE_DEPTH) return null
        (expression.evaluate() as? String)?.let { return it }
        val resolved = (expression.skipParenthesizedExprDown() as? UReferenceExpression)?.resolve() ?: return null
        constructorParameterIndex(resolved)?.let { index ->
            return (constant.toUElement() as? UEnumConstant)?.valueArguments?.getOrNull(index)?.evaluate() as? String
        }
        return stringOf(fieldAssignment(resolved), constant, depth + 1)
    }

    private fun constructorParameterIndex(element: PsiElement): Int? {
        val kotlinParameter = element.navigationElement as? KtParameter
        if (kotlinParameter != null) {
            val constructor = kotlinParameter.ownerFunction as? KtPrimaryConstructor ?: return null
            return constructor.valueParameters.indexOf(kotlinParameter).takeIf { it >= 0 }
        }
        val parameter = element as? PsiParameter ?: return null
        val constructor = (parameter.declarationScope as? PsiMethod)?.takeIf { it.isConstructor } ?: return null
        return constructor.parameterList.getParameterIndex(parameter).takeIf { it >= 0 }
    }

    private fun fieldAssignment(element: PsiElement?): UExpression? {
        val field = element as? PsiField ?: return null
        val constructor = field.containingClass?.constructors?.singleOrNull() ?: return null
        val statements = ((constructor.toUElement() as? UMethod)?.uastBody as? UBlockExpression)?.expressions.orEmpty()
        return statements.filterIsInstance<UBinaryExpression>()
            .filter { it.operator == UastBinaryOperator.ASSIGN }
            .singleOrNull { (it.leftOperand as? UResolvable)?.resolve() == field }
            ?.rightOperand
    }

    private fun PsiClassType.isResourceResponse(): Boolean {
        val resolved = resolveGenerics()
        val returnClass = resolved.element ?: return false
        val facade = JavaPsiFacade.getInstance(returnClass.project)
        val response = facade.findClass(SpringCoreClasses.ACTUATOR_WEB_ENDPOINT_RESPONSE, resolveScope) ?: return false
        if (!InheritanceUtil.isInheritorOrSelf(returnClass, response, true)) return false
        val payloadParameter = response.typeParameters.firstOrNull() ?: return false
        val payload = TypeConversionUtil.getSuperClassSubstitutor(response, returnClass, resolved.substitutor)
            .substitute(payloadParameter) ?: return false
        val resource = PsiType.getTypeByName(SpringCoreClasses.IO_RESOURCE, returnClass.project, resolveScope)
        return resource.isAssignableFrom(payload)
    }
}
