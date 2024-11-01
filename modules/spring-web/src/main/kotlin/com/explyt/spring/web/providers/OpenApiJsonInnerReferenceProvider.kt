package com.explyt.spring.web.providers

import com.explyt.spring.web.references.OpenApiJsonInnerReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class OpenApiJsonInnerReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(psiElement: PsiElement, context: ProcessingContext): Array<PsiReference> {
        return arrayOf(OpenApiJsonInnerReference(psiElement))
    }

}