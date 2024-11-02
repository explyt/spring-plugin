/*
 * Copyright © 2024 Explyt Ltd
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

package com.explyt.spring.core.providers

import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isOrdinaryClass
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class SpringImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element is PsiClass) {
            if (!element.isOrdinaryClass) {
                return false
            }
            return element.isMetaAnnotatedBy(IMPLICIT_CLASS_ANNOTATIONS)
        }
        if (element is PsiMethod) {
            if (element.isConstructor) {
                return element.isMetaAnnotatedBy(IMPLICIT_CONSTRUCTOR_ANNOTATIONS)
                        || element.containingClass?.let {
                            it.isMetaAnnotatedBy(IMPLICIT_CLASS_ANNOTATIONS)
                            && it.constructors.size == 1
                        } ?: false
            }
            if (isDynamicPropertySource(element)) {
                return true
            }
            return element.isMetaAnnotatedBy(IMPLICIT_METHOD_ANNOTATIONS)
        }
        return false
    }

    // assigned but not used
    override fun isImplicitRead(element: PsiElement): Boolean {
        return false
    }

    // referenced but never assigned
    override fun isImplicitWrite(element: PsiElement): Boolean {
        if (element is PsiField) {
            return element.isAnnotatedBy(IMPLICIT_FIELD_ANNOTATIONS)
        }
        return false
    }

    private fun isDynamicPropertySource(element: PsiMethod): Boolean {
        return /*element.isStatic && */ element.isAnnotatedBy("org.springframework.test.context.DynamicPropertySource")
    }

    companion object {
        private val IMPLICIT_CLASS_ANNOTATIONS = setOf(
            SpringCoreClasses.COMPONENT, // meta: + annotation_type
        ) + JavaEeClasses.RESOURCE.allFqns

        private val IMPLICIT_FIELD_ANNOTATIONS = setOf(
            SpringCoreClasses.AUTOWIRED, // meta: + annotation_type
            SpringCoreClasses.VALUE, // meta: + annotation_type
            // "org.springframework.beans.factory.annotation.Required", // deprecated
        ) + JavaEeClasses.INJECT.allFqns +
                JavaEeClasses.RESOURCE.allFqns

        private val IMPLICIT_CONSTRUCTOR_ANNOTATIONS = setOf(
            SpringCoreClasses.AUTOWIRED,
        ) + JavaEeClasses.INJECT.allFqns

        private val IMPLICIT_METHOD_ANNOTATIONS = setOf(
            SpringCoreClasses.BEAN, // meta: + annotation_type
            SpringCoreClasses.AUTOWIRED, // meta: + annotation_type
            SpringCoreClasses.VALUE, // meta: + annotation_type
            "org.springframework.context.event.EventListener", // meta: + annotation_type
            "org.springframework.jmx.export.annotation.ManagedOperation",
            "org.springframework.jmx.export.annotation.ManagedAttribute",
            "org.springframework.scheduling.annotation.Scheduled", // meta: + annotation_type
            "org.springframework.scheduling.annotation.Schedules", // meta: + annotation_type
            "org.springframework.test.context.transaction.BeforeTransaction", // meta: + annotation_type
            "org.springframework.test.context.transaction.AfterTransaction", // meta: + annotation_type
        ) + JavaEeClasses.INJECT.allFqns +
                JavaEeClasses.RESOURCE.allFqns +
                JavaEeClasses.POST_CONSTRUCT.allFqns +
                JavaEeClasses.PRE_DESTROY.allFqns
    }

}


