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

package com.explyt.spring.web.references.contributors

import com.explyt.spring.core.util.UastUtil
import com.explyt.spring.web.providers.UrlPathControllerMethodReferenceProvider
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.patterns.uast.callExpression
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.registerUastReferenceProvider


class UastMockMvcUrlReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val methodCall =
            callExpression()
                .withMethodNames(SpringWebUtil.REQUEST_METHODS)

        registrar.registerUastReferenceProvider(
            injectionHostUExpression(false)
                .inCall(methodCall)
                .andNot(injectionHostUExpression(false).withUastParent(UastUtil.UPolyadicExpressionPattern())),
            UrlPathControllerMethodReferenceProvider()
        )

        registrar.registerUastReferenceProvider(
            injectionHostUExpression(false)
                .withUastParent(UastUtil.UPolyadicExpressionPattern().inCall(methodCall)),
            UrlPathControllerMethodReferenceProvider()
        )
    }

}