package com.esprito.spring.data.langinjection

import com.esprito.jpa.langinjection.JpqlInjectorBase
import com.esprito.spring.data.SpringDataClasses
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType

class JpqlSpringDataQueryLanguageInjector : JpqlInjectorBase() {
    override fun isValidPlace(uElement: UElement): Boolean {
        val parentAnnotation = uElement.getParentOfType<UAnnotation>()
            ?.takeIf { it.qualifiedName == SpringDataClasses.QUERY }
            ?: return false

        return parentAnnotation
            .findAttributeValue("nativeQuery")
            ?.evaluate() != true
    }
}