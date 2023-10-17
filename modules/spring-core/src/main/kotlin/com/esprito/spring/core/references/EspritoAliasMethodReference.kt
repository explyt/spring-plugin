package com.esprito.spring.core.references

import com.esprito.spring.core.service.AliasUtils
import com.esprito.util.EspritoPsiUtil
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType

class EspritoAliasMethodReference(element: PsiElement, private val methodName: String, rangeInElement: TextRange) :
    PsiReferenceBase<PsiElement>(element, rangeInElement), PsiReference, HighlightedReference {

    override fun resolve(): PsiElement? {
        val aClass = getRelatedClass() ?: return null

        return aClass.allMethods.asSequence()
            .filter { EspritoPsiUtil.fitsForReference(it) }
            .filter { it.name == methodName }
            .firstOrNull()
    }

    override fun getVariants(): Array<Any> {
        val aClass = getRelatedClass() ?: return emptyArray()

        return aClass.allMethods.asSequence()
            .filter { EspritoPsiUtil.fitsForReference(it) }
            .map { method ->
                LookupElementBuilder.create(method.name)
                    .withIcon(AllIcons.Nodes.MethodReference)
                    .withTypeText(method.containingFile?.name)
            }.toList().toTypedArray()
    }

    private fun getRelatedClass(): PsiClass? {
        val aliasAnnotation = element
            .parentOfType<PsiAnnotation>() ?: return null

        return AliasUtils.getAliasedClass(aliasAnnotation)
    }

}