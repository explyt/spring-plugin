package com.esprito.jpa.langinjection

import com.esprito.jpa.JpaClasses
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.isUastChildOf

class JpqNamedQueryLanguageInjector : JpqlInjectorBase() {
    override fun isValidPlace(uElement: UElement): Boolean {
        val uAnnotation = uElement.getParentOfType<UAnnotation>() ?: return false

        if (!JpaClasses.namedQuery.check(uAnnotation.qualifiedName))
            return false

        val queryAttribute = uAnnotation.findAttributeValue("query")

        return uElement.isUastChildOf(queryAttribute)
    }
}