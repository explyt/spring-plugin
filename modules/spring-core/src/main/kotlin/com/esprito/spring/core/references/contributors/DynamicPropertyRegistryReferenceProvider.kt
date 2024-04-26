package com.esprito.spring.core.references.contributors

import com.esprito.spring.core.properties.references.EspritoLibraryPropertyReference
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.evaluateString

class DynamicPropertyRegistryReferenceProvider : UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourcePsi ?: return emptyArray()
        val path = uExpression.evaluateString() ?: return emptyArray()

        return arrayOf(
            EspritoLibraryPropertyReference(
                host,
                path,
                ElementManipulators.getValueTextRange(psiElement)
            )
        )
    }
}