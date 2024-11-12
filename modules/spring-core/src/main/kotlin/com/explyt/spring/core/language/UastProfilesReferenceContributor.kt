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

package com.explyt.spring.core.language.profiles

import com.explyt.spring.core.SpringCoreClasses
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression


class UastProfilesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val injection = injectionHostUExpression()
        registrar.registerUastReferenceProvider(
            injection.annotationParam(SpringCoreClasses.PROFILE, "value"), ProfileUastReferenceProvider()
        )
    }
}

private class ProfileUastReferenceProvider :
    UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val text = ElementManipulators.getValueText(host)
        val psiElement = uExpression.sourcePsi ?: return PsiReference.EMPTY_ARRAY
        val profileRanges = ProfilesUtil.getProfileRanges(text)

        val referenceOffset: Int = ElementManipulators.getOffsetInElement(psiElement)
        return profileRanges.map {
            val profileName = it.substring(text)
            SpringProfilePsiReference(psiElement, profileName, true, it.shiftRight(referenceOffset))
        }.toTypedArray()
    }
}

