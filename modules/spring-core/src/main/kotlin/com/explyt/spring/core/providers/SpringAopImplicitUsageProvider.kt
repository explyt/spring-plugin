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

import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class SpringAopImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        if (element is PsiMethod) {
            return !element.isConstructor
                    && element.isAnnotatedBy(IMPLICIT_METHOD_ANNOTATIONS)
        }
        return false

    }

    override fun isImplicitRead(element: PsiElement): Boolean {
        return false
    }

    override fun isImplicitWrite(element: PsiElement): Boolean {
        return false
    }

    companion object {
        private val IMPLICIT_METHOD_ANNOTATIONS = listOf(
            "org.aspectj.lang.annotation.Before",
            "org.aspectj.lang.annotation.After",
            "org.aspectj.lang.annotation.Around",
            "org.aspectj.lang.annotation.AfterReturning",
            "org.aspectj.lang.annotation.AfterThrowing"
        )

    }
}