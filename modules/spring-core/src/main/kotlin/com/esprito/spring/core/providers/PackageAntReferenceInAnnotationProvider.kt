package com.esprito.spring.core.providers

import com.esprito.spring.core.references.InAnnotationPackageAntReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

class PackageAntReferenceInAnnotationProvider(private val possibleAnnotations: Set<String>) : PsiReferenceProvider() {
    override fun getReferencesByElement(
        psiElement: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val uAnnotation = psiElement.toUElement()?.getParentOfType<UAnnotation>() ?: return PsiReference.EMPTY_ARRAY
        if (!acceptPsiElement(uAnnotation)) return PsiReference.EMPTY_ARRAY

        val text = ElementManipulators.getValueText(psiElement)
        val words = text.split(".")

        val references = mutableListOf<PsiReference>()

        var path = ""
        for (word in words) {
            path += word
            references.add(
                InAnnotationPackageAntReference(
                    psiElement,
                    TextRange(
                        path.length - word.length,
                        path.length
                    ).shiftRight(ElementManipulators.getValueTextRange(psiElement).startOffset)
                )
            )
            path += "."
        }

        return references.toTypedArray()
    }

    private fun acceptPsiElement(uAnnotation: UAnnotation): Boolean {
        return possibleAnnotations.contains(uAnnotation.qualifiedName)
    }
}