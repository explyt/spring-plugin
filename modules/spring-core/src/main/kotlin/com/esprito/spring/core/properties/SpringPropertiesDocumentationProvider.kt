package com.esprito.spring.core.properties

import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.psi.PsiElement

class SpringPropertiesDocumentationProvider : ConfigurationPropertiesDocumentationProvider<PropertyKeyImpl>() {

    override fun extractOriginalElement(originalElement: PsiElement?): PropertyKeyImpl? {
        return originalElement as? PropertyKeyImpl
    }

    override fun getPropertyFullKey(extractedOriginalElement: PropertyKeyImpl): String {
        return extractedOriginalElement.text
    }
}
