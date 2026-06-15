/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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

private class ProfileUastReferenceProvider : UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val text = ElementManipulators.getValueText(host)
        val psiElement = uExpression.sourcePsi ?: return PsiReference.EMPTY_ARRAY
        val profileRanges = ProfilesUtil.parseProfiles(text)

        val referenceOffset: Int = ElementManipulators.getOffsetInElement(psiElement)
        return profileRanges.map {
            val profileName = it.substring(text)
            ProfilePsiReference(psiElement, profileName, it.shiftRight(referenceOffset))
        }.toTypedArray()
    }
}

