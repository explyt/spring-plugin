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

import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.PATHS
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringOpenApiYamlUrlEndpointReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(psiElement: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val keyElement = psiElement as? YAMLKeyValue ?: return emptyArray()
        val parentElement = psiElement.parentOfType<YAMLKeyValue>(false) ?: return emptyArray()
        if (YAMLUtil.getConfigFullName(parentElement) == PATHS) {
            return getReferenceForUrl(keyElement)
        }

        return getReferenceForRequestMethod(keyElement, parentElement)
    }

    private fun getReferenceForUrl(keyElement: YAMLKeyValue): Array<PsiReference> {
        val key = YAMLUtil.getConfigFullNameParts(keyElement).lastOrNull() ?: return emptyArray()

        return arrayOf(
            ExplytControllerMethodReference(
                keyElement,
                key,
                null,
                ElementManipulators.getValueTextRange(keyElement),
                true
            )
        )
    }

    private fun getReferenceForRequestMethod(keyElement: YAMLKeyValue, urlElement: YAMLKeyValue): Array<PsiReference> {
        val url = YAMLUtil.getConfigFullNameParts(urlElement).lastOrNull() ?: return emptyArray()
        val key = YAMLUtil.getConfigFullNameParts(keyElement).lastOrNull() ?: return emptyArray()
        if (key !in SpringWebUtil.REQUEST_METHODS) return emptyArray()
        val pathElement = urlElement.parentOfType<YAMLKeyValue>() ?: return emptyArray()
        if (YAMLUtil.getConfigFullName(pathElement) != PATHS) return emptyArray()

        return arrayOf(
            ExplytControllerMethodReference(
                keyElement,
                url,
                key.uppercase(),
                ElementManipulators.getValueTextRange(keyElement),
                true
            )
        )
    }

}