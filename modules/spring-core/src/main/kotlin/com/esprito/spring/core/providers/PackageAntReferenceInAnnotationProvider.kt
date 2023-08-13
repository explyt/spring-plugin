package com.esprito.spring.core.providers

import com.esprito.spring.core.references.InAnnotationPackageAntReference
import com.esprito.spring.core.util.PsiAnnotationUtils
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.ProcessingContext

class PackageAntReferenceInAnnotationProvider(private val possibleAnnotations: Set<String>) : PsiReferenceProvider() {
    override fun getReferencesByElement(
        psiElement: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        if (!acceptPsiElement(psiElement)) return PsiReference.EMPTY_ARRAY
        if (psiElement !is PsiLiteral) return PsiReference.EMPTY_ARRAY

        val text = psiElement.value.toString()
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

    private fun acceptPsiElement(psiElement: PsiElement): Boolean {
        return possibleAnnotations.contains(PsiAnnotationUtils.getParentAnnotationForPsiLiteralParameter(psiElement)?.qualifiedName)
    }
}