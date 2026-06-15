/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.language.profiles

import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

class YamlProfilesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLKeyValue::class.java),
            SpringYamlProfilesReferenceProvider()
        )
    }
}

class SpringYamlProfilesReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val yamlKeyValue = element as? YAMLKeyValue ?: return PsiReference.EMPTY_ARRAY

        val propertyKey = YAMLUtil.getConfigFullName(yamlKeyValue)
        val propertyValue = yamlKeyValue.value ?: return PsiReference.EMPTY_ARRAY
        if (propertyKey != SPRING_PROFILES_ACTIVE) return PsiReference.EMPTY_ARRAY

        val result = mutableListOf<PsiReference>()

        val matcher = ProfilesUtil.profilePattern.matcher(propertyValue.text)
        while (matcher.find()) {
            val text = matcher.group()
            val range = TextRange.allOf(text).shiftRight(matcher.start()).shiftRight(propertyValue.startOffsetInParent)
            result.add(ProfilePsiReference(yamlKeyValue, text, range))
        }

        return result.toTypedArray()
    }

}

