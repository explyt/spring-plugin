package com.esprito.spring.core.references

import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.util.ExplytPsiUtil
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUastParentOfType

class ExplytMethodReference(element: PsiElement, private val methodName: String, rangeInElement: TextRange) :
    PsiReferenceBase<PsiElement>(element, rangeInElement), PsiReference, HighlightedReference {

    override fun resolve(): PsiElement? {
        val aClass = getRelatedClass() ?: return null

        return aClass.allMethods.asSequence()
            .filter { ExplytPsiUtil.fitsForReference(it) }
            .filter { it.name == methodName }
            .firstOrNull()
    }

    override fun getVariants(): Array<Any> {
        val aClass = getRelatedClass() ?: return emptyArray()

        return aClass.allMethods.asSequence()
            .filter { ExplytPsiUtil.fitsForReference(it) }
            .map { method ->
                LookupElementBuilder.create(method.name)
                    .withIcon(AllIcons.Nodes.MethodReference)
                    .withTypeText(method.containingFile?.name)
            }.toList().toTypedArray()
    }

    private fun getRelatedClass(): PsiClass? {
        val uastParent = element
            .getUastParentOfType<UAnnotation>()
            ?.uastParent ?: return null

        return when (uastParent) {
            is UClass -> uastParent.javaPsi
            is UMethod -> uastParent.javaPsi.returnType?.resolveBeanPsiClass
            else -> null
        }
    }

}