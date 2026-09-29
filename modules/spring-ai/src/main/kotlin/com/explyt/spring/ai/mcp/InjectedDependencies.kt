/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.providers.SpringBeanLineMarkerProvider
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.psi.KtParameter

/**
 * The dependencies a Spring container hands a bean, as opposed to the objects the bean builds for itself.
 *
 * A field counts as injected when it is `@Autowired`/`@Inject`/`@Resource`, or when it has no initializer and a
 * constructor fills it from one of its parameters - the constructor injection Spring performs for a single
 * constructor, including a Kotlin primary-constructor property.
 */
internal object InjectedDependencies {

    fun isInjected(field: PsiField): Boolean {
        if (SpringBeanLineMarkerProvider.isAutowiredFieldExpression(field)) return true
        if (field.initializer != null) return false
        val constructors = field.containingClass?.constructors ?: return false
        if ((field as? KtLightField)?.kotlinOrigin is KtParameter) {
            return constructors.any { constructor ->
                constructor.parameterList.parameters.any { it.name == field.name && it.type == field.type }
            }
        }
        return constructors.any { constructor ->
            constructor.body?.statements?.any { statement ->
                val assignment = (statement as? PsiExpressionStatement)?.expression as? PsiAssignmentExpression
                assignment != null &&
                        (assignment.lExpression as? PsiReferenceExpression)?.resolve() == field &&
                        (assignment.rExpression as? PsiReferenceExpression)?.resolve() in constructor.parameterList.parameters
            } == true
        }
    }

    /**
     * The injected field of [owner] a call receiver resolves to, or `null` when the receiver is anything else.
     *
     * Inside a Kotlin class a primary-constructor property may resolve to the constructor parameter declaring it rather
     * than to the field, so a parameter of one of [owner]'s constructors is mapped to the field of the same name.
     */
    fun fieldOf(receiver: PsiElement?, owner: PsiClass): PsiField? {
        val field = when (receiver) {
            is PsiField -> receiver
            is PsiParameter -> {
                val constructor = (receiver.declarationScope as? PsiMethod)?.takeIf { it.isConstructor } ?: return null
                constructor.containingClass?.findFieldByName(receiver.name, false)?.takeIf { it.type == receiver.type }
            }
            else -> null
        } ?: return null
        val declaringClass = field.containingClass ?: return null
        return field.takeIf { InheritanceUtil.isInheritorOrSelf(owner, declaringClass, true) && isInjected(it) }
    }
}
