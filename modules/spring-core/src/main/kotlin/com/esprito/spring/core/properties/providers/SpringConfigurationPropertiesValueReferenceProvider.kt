package com.esprito.spring.core.properties.providers

import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.esprito.spring.core.SpringProperties.PLACEHOLDER_SUFFIX
import com.esprito.spring.core.properties.references.PlaceholderValueReference
import com.esprito.spring.core.properties.references.ValueHintReference
import com.esprito.spring.core.util.PropertyUtil.propertyKey
import com.esprito.spring.core.util.PropertyUtil.propertyValuePsiElement
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.intellij.util.text.PlaceholderTextRanges
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
        val ranges = PlaceholderTextRanges.getPlaceholderRanges(
            text,
            PLACEHOLDER_PREFIX,
            PLACEHOLDER_SUFFIX
        )
        val result = mutableListOf<PsiReference>()
        for (it in ranges) {
            val index = it.substring(text).indexOf(SpringProperties.COLON)
            val textInRange =
                if (index == -1) it.substring(text) else it.substring(text).substringBefore(SpringProperties.COLON)
            val range = if (index == -1) it else TextRange.from(it.startOffset, index)
            result.add(PlaceholderValueReference(textInRange, element, range))
        }
        return result
    }
}