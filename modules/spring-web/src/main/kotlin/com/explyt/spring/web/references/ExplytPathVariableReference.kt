package com.explyt.spring.web.references

import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.getUastParentOfType

class ExplytPathVariableReference(element: PsiElement, private val methodName: String, rangeInElement: TextRange) :
    PsiReferenceBase<PsiElement>(element, rangeInElement), PsiReference, HighlightedReference {

    override fun resolve(): PsiElement? {
        val method = getAnnotatedMethod() ?: return null
        return SpringWebUtil.collectPathVariables(method)
            .firstOrNull {
                it.name == methodName
            }
            ?.psiElement
    }

    override fun getVariants(): Array<Any> {
        val method = getAnnotatedMethod() ?: return emptyArray()
        return SpringWebUtil.collectPathVariables(method)
            .mapTo(mutableListOf()) {
                LookupElementBuilder.create(it.name)
            }.toTypedArray()

    }

    private fun getAnnotatedMethod(): PsiMethod? {
        val uastParent = element
            .getUastParentOfType<UAnnotation>()
            ?.uastParent ?: return null

        return uastParent.javaPsi as? PsiMethod
    }

}