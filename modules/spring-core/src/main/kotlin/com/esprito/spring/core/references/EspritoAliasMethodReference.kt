package com.esprito.spring.core.references

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.util.EspritoPsiUtil
import com.esprito.util.EspritoPsiUtil.findChildrenOfType
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.getUastParentOfType

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
            .getUastParentOfType<UAnnotation>() ?: return null

        val annotationReference = aliasAnnotation.findAttributeValue("annotation")?.javaPsi ?: return null

        val resolvedClassReference =
            annotationReference.findChildrenOfType<PsiTypeElement>()
                .firstOrNull { it.type.resolvedPsiClass?.qualifiedName != SpringCoreClasses.ANNOTATION }
                ?.type?.resolvedPsiClass
        if (resolvedClassReference != null) return resolvedClassReference

        return aliasAnnotation.javaPsi?.parentOfType<PsiClass>()
    }

}