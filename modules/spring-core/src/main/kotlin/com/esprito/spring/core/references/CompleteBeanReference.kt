package com.esprito.spring.core.references

import com.esprito.spring.core.service.SpringBeanService
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.uast.*

class CompleteBeanReference(element: PsiElement, private val beanName: String, textRange: TextRange) :
    PsiReferenceBase<PsiElement>(element, textRange), PsiReference , HighlightedReference {

    override fun resolve(): PsiElement? {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        val springSearchService = SpringSearchService.getInstance(element.project)
        val foundBeanDeclarations = springSearchService.findBeanDeclarations(module, beanName)
        val resolveResults = foundBeanDeclarations.map { PsiElementResolveResult(it) }.toTypedArray()
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        val variants = HashSet<Any>()

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val resolvedPsiClass = getResolvePsiClass() ?: return emptyArray()
        val beanCandidates = SpringBeanService.getInstance(module.project).getBeanCandidates(resolvedPsiClass, module)

        beanCandidates.mapTo(variants) {
            LookupElementBuilder.create(it.name)
                .withIcon(AllIcons.Nodes.Class)
                .withTailText(" (${it.psiClass.containingFile?.name})")
                .withTypeText(it.psiClass.name)
        }
        return variants.toTypedArray()
    }

    private fun getResolvePsiClass(): PsiClass? {
        val uastParent = element
            .getUastParentOfType<UAnnotation>()
            ?.uastParent ?: return null

        return when (uastParent) {
            is UField -> uastParent.type.resolveBeanPsiClass
            is UParameter -> uastParent.type.resolveBeanPsiClass
            else -> null
        }
    }

}
