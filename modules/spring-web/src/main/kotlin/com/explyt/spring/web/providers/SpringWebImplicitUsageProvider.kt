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

package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytPsiUtil.inClassMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isStatic
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

class SpringWebImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitWrite(element: PsiElement): Boolean {
        return element is PsiParameter && isImplicitParameter(element)
    }

    override fun isImplicitRead(element: PsiElement): Boolean {
        return false
    }

    override fun isImplicitUsage(element: PsiElement): Boolean {
        return when (element) {
            is PsiClass -> {
                return element.isAnnotatedBy(IMPLICIT_CLASS_ANNOTATIONS)
            }

            is PsiMethod -> {
                return !element.isStatic && !element.isConstructor
                        && element.inClassMetaAnnotatedBy(SpringCoreClasses.COMPONENT) // or bean
                        && element.isMetaAnnotatedBy(IMPLICIT_METHOD_ANNOTATIONS)
            }

            else -> false
        }
    }

    private fun isImplicitParameter(element: PsiParameter): Boolean {
        return element.isAnnotatedBy(IMPLICIT_PARAMETER_ANNOTATIONS)
                || element.isAnnotatedBy(SpringWebClasses.MODEL_ATTRIBUTE)
    }

    companion object {
        val IMPLICIT_CLASS_ANNOTATIONS = setOf(
            "jakarta.servlet.annotation.WebFilter",
            "jakarta.servlet.annotation.WebListener",
            "jakarta.servlet.annotation.WebServlet",
            "javax.servlet.annotation.WebFilter",
            "javax.servlet.annotation.WebListener",
            "javax.servlet.annotation.WebServlet",
        )
        val IMPLICIT_PARAMETER_ANNOTATIONS: Collection<String> = listOf(
            "org.springframework.web.bind.annotation.PathVariable",
            "org.springframework.web.bind.annotation.RequestParam"
        )
        val IMPLICIT_METHOD_ANNOTATIONS = setOf(
            "org.springframework.web.bind.annotation.ExceptionHandler",
            "org.springframework.web.bind.annotation.InitBinder",
            "org.springframework.web.bind.annotation.RequestMapping",
            SpringWebClasses.MODEL_ATTRIBUTE,
            "org.springframework.graphql.data.method.annotation.QueryMapping",
            "org.springframework.graphql.data.method.annotation.MutationMapping",
            "org.springframework.graphql.data.method.annotation.BatchMapping",
            "org.springframework.graphql.data.method.annotation.SubscriptionMapping",
            "org.springframework.graphql.data.method.annotation.SchemaMapping", // + type
        )

    }

}


