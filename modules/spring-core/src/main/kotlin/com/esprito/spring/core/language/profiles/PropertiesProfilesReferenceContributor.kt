package com.esprito.spring.core.language.profiles

import com.esprito.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

class PropertiesProfilesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyValueImpl::class.java),
            SpringPropertiesProfilesReferenceProvider()
        )
    }
}

class SpringPropertiesProfilesReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val propertyValue = element as? PropertyValueImpl ?: return PsiReference.EMPTY_ARRAY
        val propertyKey = element.parentOfType<PropertyImpl>()?.key ?: return PsiReference.EMPTY_ARRAY
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return emptyArray()
        }
        if (propertyKey != SPRING_PROFILES_ACTIVE) return PsiReference.EMPTY_ARRAY

        val result = mutableListOf<PsiReference>()

        val matcher = ProfilesUtil.profilePattern.matcher(propertyValue.text)
        while (matcher.find()) {
            val text = matcher.group()
            result.add(
                SpringProfilePsiReference(
                    propertyValue, text, false, TextRange.allOf(text).shiftRight(matcher.start())
                )
            )
        }

        return result.toTypedArray()

    }

}
