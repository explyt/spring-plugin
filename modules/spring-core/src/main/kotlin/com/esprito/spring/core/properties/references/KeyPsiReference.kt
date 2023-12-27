package com.esprito.spring.core.properties.references

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.completion.properties.ValueHint
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.yaml.psi.YAMLKeyValue

class KeyPsiReference(
    element: PsiElement,
    textRange: TextRange,
    val prefix: String,
    private val valueHint: ValueHint
) : PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement {
        return this.element
    }

    override fun getVariants(): Array<Any> {
        val lookupString = if (this.element is YAMLKeyValue) {
            "  ${valueHint.value}: "
        } else if (this.element is PropertyKeyImpl) {
            "$prefix.${valueHint.value}"
        } else {
            null
        } ?: return emptyArray()

        return arrayOf(
            LookupElementBuilder.create(lookupString)
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