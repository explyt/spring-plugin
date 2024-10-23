package com.esprito.spring.web.providers

import com.esprito.spring.web.references.OpenApiYamlInnerReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class OpenApiYamlInnerReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(psiElement: PsiElement, context: ProcessingContext): Array<PsiReference> {
        return arrayOf(OpenApiYamlInnerReference(psiElement))
    }

}