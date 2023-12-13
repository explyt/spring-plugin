package com.esprito.spring.core.properties

import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.esprito.spring.core.SpringProperties.PLACEHOLDER_SUFFIX
import com.esprito.spring.core.properties.reference.PlaceholderValueReference
import com.esprito.spring.core.properties.reference.ValueHintReference
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.intellij.util.text.PlaceholderTextRanges

class SpringConfigurationPropertiesValueReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val propertyKey = element.parentOfType<PropertyImpl>()?.key ?: return emptyArray()
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return emptyArray()
        }
        val placeholderReferences = getPlaceholderValueReferences(element)
        if (placeholderReferences.isNotEmpty()) {
            return placeholderReferences.toTypedArray()
        }
        return arrayOf(ValueHintReference(element, propertyKey))
    }

    private fun getPlaceholderValueReferences(element: PsiElement): List<PsiReference> {
        val text = ElementManipulators.getValueText(element)
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