package com.esprito.spring.core.references

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext

class PackageReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    if (element !is PsiLiteralExpression
                        || element.parent == null
                        || element.parent !is PsiNameValuePair
                        || element.parent.firstChild.text != "basePackages"
                    ) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    val text = element.value.toString()
                    val words = text.split(".")

                    val references = mutableListOf<PsiReference>()

                    var path = ""
                    for (word in words) {
                        path += word
                        references.add(
                            EspritoPsiPackageReference(
                                element,
                                TextRange(
                                    path.length - word.length,
                                    path.length
                                ).shiftRight(ElementManipulators.getValueTextRange(element).startOffset)
                            )
                        )
                        path += "."
                    }

                     return references.toTypedArray()
                }
            }
        )
    }

}