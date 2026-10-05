/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.PropertyUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor

/**
 * Whether a method does nothing but read or write one field of its class, so that a call trace lists it and moves on.
 *
 * A service building a response reads a DTO field by field, and each getter is a project method: following all of
 * them spent the trace's method limit on sixty `getX` nodes and left the repository call it was made for untraced.
 *
 * Triviality is proven by the body or by the way the method was generated, never by the name:
 * `PropertyUtilBase.isSimplePropertyAccessor` accepts any `getX()` by its signature alone, and the `getTotal` that
 * multiplies two fields, the Kotlin property with a `get()` body or a delegate, the member a class delegates to another
 * object with `by`, and the Lombok getter that computes lazily all share that signature and are traced as any method.
 */
internal object TrivialAccessors {

    fun isTrivial(method: PsiMethod): Boolean = when {
        method.hasModifierProperty(PsiModifier.ABSTRACT) -> false
        method is KtLightMethod -> isDefaultPropertyAccessor(method)
        else -> readsOrWritesOneField(method) || isGeneratedFromAField(method)
    }

    private fun readsOrWritesOneField(method: PsiMethod): Boolean =
        PropertyUtil.getFieldOfGetter(method) != null || PropertyUtil.getFieldOfSetter(method) != null

    private fun isDefaultPropertyAccessor(method: KtLightMethod): Boolean =
        !method.isDelegated && when (val origin = method.kotlinOrigin) {
            is KtParameter -> origin.hasValOrVar()
            is KtPropertyAccessor -> !origin.hasBody()
            is KtProperty -> !origin.hasDelegate() && origin.accessorOf(method)?.hasBody() != true
            else -> false
        }

    private fun isGeneratedFromAField(method: PsiMethod): Boolean {
        if (method.isPhysical) return false
        val field = method.navigationElement as? PsiField ?: return false
        val owner = method.containingClass ?: return false
        return field.containingClass?.isEquivalentTo(owner) == true &&
                !computesItsValue(field) &&
                isNamedAfter(method, field) &&
                (readsField(method, field) || writesField(method, field))
    }

    private fun computesItsValue(field: PsiField): Boolean =
        AnnotationUtil.isAnnotated(field, LOMBOK_DELEGATE, 0) ||
                AnnotationUtil.findAnnotation(field, LOMBOK_GETTER)
                    ?.let { AnnotationUtil.getBooleanAttributeValue(it, "lazy") } == true

    private fun isNamedAfter(method: PsiMethod, field: PsiField): Boolean {
        val property = field.name.replaceFirstChar(Char::uppercaseChar)
        return method.name == field.name || method.name in listOf("get$property", "is$property", "set$property")
    }

    private fun readsField(method: PsiMethod, field: PsiField): Boolean =
        method.parameterList.isEmpty && method.returnType == field.type

    private fun writesField(method: PsiMethod, field: PsiField): Boolean {
        val parameter = method.parameterList.parameters.singleOrNull() ?: return false
        val returnType = method.returnType
        return parameter.type == field.type && (
                returnType == null || isSetter(method) ||
                        PsiUtil.resolveClassInClassTypeOnly(returnType)?.isEquivalentTo(method.containingClass) == true
                )
    }

    private fun KtProperty.accessorOf(method: PsiMethod): KtPropertyAccessor? = if (isSetter(method)) setter else getter

    private fun isSetter(method: PsiMethod): Boolean = method.returnType == PsiTypes.voidType()

    private const val LOMBOK_GETTER = "lombok.Getter"
    private const val LOMBOK_DELEGATE = "lombok.experimental.Delegate"
}
