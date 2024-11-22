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

package com.explyt.util

import com.intellij.patterns.uast.UExpressionPattern
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.psi.registerUastReferenceProvider
import org.jetbrains.uast.UExpression

object ExplytContributorUtil {

    fun addAnnotationValueContributor(
        registrar: PsiReferenceRegistrar,
        injection: UExpressionPattern<UExpression, *>,
        className: String,
        provider: UastInjectionHostReferenceProvider
    ) {
        addAnnotationValueContributor(registrar, injection, className, provider, listOf("value"))
    }

    fun addAnnotationValueContributor(
        registrar: PsiReferenceRegistrar,
        injection: UExpressionPattern<UExpression, *>,
        className: String,
        provider: UastInjectionHostReferenceProvider,
        parameterNames: List<String>
    ) {
        parameterNames.forEach {
            registrar.registerUastReferenceProvider(
                injection.annotationParam(className, it), provider, PsiReferenceRegistrar.LOWER_PRIORITY
            )
        }
    }
}