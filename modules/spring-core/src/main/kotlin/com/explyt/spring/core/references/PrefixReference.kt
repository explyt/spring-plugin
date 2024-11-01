package com.explyt.spring.core.references

import com.explyt.spring.core.SpringProperties
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

enum class PrefixReferenceType {
    ANNOTATION_PROPERTY,
    FILE_PROPERTY
}

class PrefixReference(
    element: PsiElement,
    textRange: TextRange,
    private val type: PrefixReferenceType
) : PsiReferenceBase<PsiElement>(element, textRange, false) {

    override fun resolve(): PsiElement {
        return this.element
    }

    override fun getVariants(): Array<Any> {
        return when (type) {
            PrefixReferenceType.ANNOTATION_PROPERTY -> {
                arrayOfAnnotationProperties()
            }
            PrefixReferenceType.FILE_PROPERTY -> {
                arrayOfAll()
            }
        }
    }

    private fun arrayOfAnnotationProperties(): Array<Any> {
        return arrayOf(
            LookupElementBuilder.create(SpringProperties.PREFIX_CLASSPATH).bold(),
            LookupElementBuilder.create(SpringProperties.PREFIX_FILE).bold(),
        )
    }

    private fun arrayOfAll(): Array<Any> {
        return arrayOf(
            LookupElementBuilder.create(SpringProperties.PREFIX_CLASSPATH_STAR).bold(),
            LookupElementBuilder.create(SpringProperties.PREFIX_CLASSPATH).bold(),
            LookupElementBuilder.create(SpringProperties.PREFIX_FILE).bold(),
            LookupElementBuilder.create(SpringProperties.PREFIX_HTTP).bold(),
        )
    }
}

