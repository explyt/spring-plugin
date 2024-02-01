package com.esprito.jpa.ql.reference

import com.esprito.jpa.ql.psi.JpqlFullyQualifiedConstructor
import com.esprito.jpa.ql.psi.JpqlIdentifier
import com.esprito.jpa.ql.psi.impl.JpqlElementFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType

class JpqlFullyQualifiedElementReference(
    identifier: JpqlIdentifier
) :
    PsiPolyVariantReferenceBase<JpqlIdentifier>(
        identifier,
        TextRange(0, identifier.textLength)
    ) {
    private val referenceProvider = JavaClassReferenceProvider()

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        return getElementReference()?.variants ?: return emptyArray()
    }

    override fun multiResolve(incompleteCode: Boolean): Array<PsiElementResolveResult> {
        return getElementReference()?.let {
            it.resolve()?.let { referencedElement ->
                arrayOf(PsiElementResolveResult(referencedElement))
            }
        } ?: emptyArray()
    }

    private fun getElementReference(): PsiReference? {
        val fqConstructor = element.parentOfType<JpqlFullyQualifiedConstructor>() ?: return null

        var currentElement: JpqlIdentifier? = element
        var countToSkip = -1
        while (currentElement != null) {
            currentElement = PsiTreeUtil.getPrevSiblingOfType(currentElement, JpqlIdentifier::class.java)
            countToSkip++
        }

        return JavaClassReferenceSet(
            fqConstructor.text,
            fqConstructor,
            0,
            false,
            referenceProvider
        ).references.asSequence()
            .drop(countToSkip)
            .firstOrNull()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val newConstructorElement = JpqlElementFactory.getInstance(element.project)
            .createIdentifier(newElementName)

        return element.replace(newConstructorElement)
    }

}