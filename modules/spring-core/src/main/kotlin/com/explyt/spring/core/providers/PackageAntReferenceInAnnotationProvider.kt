package com.explyt.spring.core.providers

import com.explyt.spring.core.references.PackageAntReferenceSet
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.psi.search.ProjectScope
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.sourceInjectionHost

class PackageAntReferenceInAnnotationProvider : UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourceInjectionHost ?: return emptyArray()

        return PackageAntReferenceSet(
            ElementManipulators.getValueText(psiElement),
            psiElement,
            ElementManipulators.getValueTextRange(psiElement).startOffset,
            ProjectScope.getContentScope(psiElement.project)
        ).psiReferences
    }

}