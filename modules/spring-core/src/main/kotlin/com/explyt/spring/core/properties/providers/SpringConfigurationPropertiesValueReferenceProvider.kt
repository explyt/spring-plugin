package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.properties.references.ExplytPropertyReference
import com.explyt.spring.core.properties.references.ValueHintReference
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.propertyKey
import com.explyt.spring.core.util.PropertyUtil.propertyValuePsiElement
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.intellij.util.text.findTextRange

class SpringConfigurationPropertiesValueReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return emptyArray()
        }
        val placeholderReferences = getPlaceholderValueReferences(element)
        if (placeholderReferences.isNotEmpty()) {
            return placeholderReferences.toTypedArray()
        }
        if (element.propertyKey() == SpringProperties.SPRING_PROFILES_ACTIVE) {
            return PsiReference.EMPTY_ARRAY
        }

        val valueElement = element.propertyValuePsiElement() ?: return emptyArray()
        val textRange = element.text.findTextRange(valueElement.text) ?: return emptyArray()
        return arrayOf(ValueHintReference(element, textRange))
    }

    private fun getPlaceholderValueReferences(element: PsiElement): List<PsiReference> {
        val text = element.text ?: return emptyList()

        return PropertyUtil.getPlaceholders(text) { placeholder, range ->
            ExplytPropertyReference(element, placeholder, range, true)
        }
    }
}