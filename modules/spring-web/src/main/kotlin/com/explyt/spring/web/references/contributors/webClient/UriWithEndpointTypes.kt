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

package com.explyt.spring.web.references.contributors.webClient

import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.tryResolve

data class UriWithEndpointTypes(val uri: String, val endpointTypes: Sequence<PsiClassReferenceType>) {
    companion object {
        fun endpointsTypesForExpression(
            referenceExpression: UQualifiedReferenceExpression?,
            responseSpec: String,
            uriSpec: String,
            module: Module
        ): UriWithEndpointTypes? {
            var receiver = referenceExpression
            if (receiver?.getExpressionType()?.canonicalText != responseSpec) return null

            var uri: String? = null
            var method: String? = null

            while (receiver != null && (uri == null || method == null)) {
                val psiMethod = receiver.tryResolve() as? PsiMethod
                if (psiMethod != null) {
                    val methodName = psiMethod.name
                    if (method == null && methodName in SpringWebUtil.REQUEST_METHODS) {
                        method = methodName.uppercase()
                    }

                    if (uri == null && methodName == "uri" && psiMethod.containingClass?.qualifiedName == uriSpec) {
                        uri = (receiver.selector as? UCallExpression)
                            ?.getArgumentForParameter(0)
                            ?.evaluateString()
                    }
                }
                receiver = receiver.receiver as? UQualifiedReferenceExpression
            }
            if (uri == null || method == null) return null

            val endpointTypes = SpringWebEndpointsSearcher.getInstance(module.project)
                .getAllEndpointElements(uri.replace("\"", ""), module)
                .asSequence()
                .filter { it.requestMethods.contains(method) }
                .mapNotNull { it.psiElement as? PsiMethod }
                .mapNotNull { it.returnType as? PsiClassReferenceType }

            return UriWithEndpointTypes(uri, endpointTypes)
        }

    }
}
