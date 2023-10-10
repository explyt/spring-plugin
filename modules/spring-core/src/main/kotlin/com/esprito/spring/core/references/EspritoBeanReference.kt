package com.esprito.spring.core.references

import com.esprito.spring.core.service.SpringSearchService
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

class EspritoBeanReference(
    element: PsiElement,
    private val beanName: String,
    rangeInElement: TextRange
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference, HighlightedReference {

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val springSearchService = SpringSearchService.getInstance(element.project)
        val foundBeanDeclarations = springSearchService.findActiveBeanDeclarations(module, beanName)
        return foundBeanDeclarations.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val resolveResults: Array<out ResolveResult> = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        val project: Project = myElement.project
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val allBeans = SpringSearchService.getInstance(project).getActiveBeansClasses(module)
        return allBeans.map { bean ->
            LookupElementBuilder.create(bean.name)
                .withIcon(AllIcons.Nodes.Class)
                .withTypeText(bean.psiClass.containingFile?.name)
        }.toTypedArray()
    }
}