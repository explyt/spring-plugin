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

