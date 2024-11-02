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

import com.explyt.spring.core.references.contributors.CommonAnnotationReferenceProvider
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.references.ExplytPathVariableReference
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*

class UastRequestMappingReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(injectionHostUExpression(), PathVariableReferenceProvider())
    }
}

class PathVariableReferenceProvider : CommonAnnotationReferenceProvider(annotationToMethodProperties) {

    override fun getReference(
        host: PsiLanguageInjectionHost,
        valueText: String,
        rangeInElement: TextRange
    ): PsiReference = ExplytPathVariableReference(host, valueText, rangeInElement)

    override fun getReferences(
        host: PsiLanguageInjectionHost,
        valueText: String,
        rangeInElement: TextRange
    ): Collection<PsiReference> {
        val matches = SpringWebUtil.NameInBracketsRx.findAll(valueText)

        return matches
            .mapNotNull { it.groups["name"] }
            .mapTo(mutableListOf()) {
                val range = TextRange(rangeInElement.startOffset + it.range.first, it.range.last + 2)
                getReference(host, it.value, range)
            }
    }

    companion object {
        val annotationToMethodProperties = mapOf(
            SpringWebClasses.REQUEST_MAPPING to setOf("path", "value"),
        )
    }

}