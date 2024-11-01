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

import com.explyt.spring.core.util.UastUtil.getArgumentValueAsEnumName
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

class WebClientUrlPathControllerMethodReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val uFullExpression = uExpression.uastParent as? UPolyadicExpression ?: uExpression
        val uCallExpression = uFullExpression.uastParent as? UCallExpression ?: return emptyArray()
        val callReceiver = uCallExpression.receiver ?: return emptyArray()

        val uMethodCall = callReceiver.getUCallExpression() ?: return emptyArray()
        var requestMethod = uMethodCall.methodName ?: return emptyArray()
        if (requestMethod == "method") {
            requestMethod = uMethodCall.getArgumentValueAsEnumName(0) ?: return emptyArray()
        }

        val text = uFullExpression.evaluateString() ?: return emptyArray()
        if (text.isBlank()) return emptyArray()

        return arrayOf(
            ExplytControllerMethodReference(
                host,
                text,
                requestMethod.uppercase(),
                ElementManipulators.getValueTextRange(host)
            )
        )
    }

}