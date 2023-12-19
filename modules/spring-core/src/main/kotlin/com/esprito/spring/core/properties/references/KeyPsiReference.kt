package com.esprito.spring.core.properties.references

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.completion.properties.ValueHint
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class KeyPsiReference(
    element: PsiElement,
    val prefix: String,
    private val valueHint: ValueHint
) : PsiReferenceBase<PsiElement>(element) {

    override fun resolve(): PsiElement {
        return this.element
    }

    override fun getVariants(): Array<Any> {
        return arrayOf(
            LookupElementBuilder.create("$prefix.${valueHint.value}")
                .withRenderer(HintValuePropertyRenderer(valueHint))
        )
    }
}

class HintValuePropertyRenderer(private val valueHint: ValueHint) : LookupElementRenderer<LookupElement>() {
    override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
        presentation.itemText = valueHint.value
        val description = valueHint.description
        if (!description.isNullOrBlank()) {
            presentation.setTailText(" ($description)", true)
        }
        presentation.icon = SpringIcons.PropertyKey
    }
}