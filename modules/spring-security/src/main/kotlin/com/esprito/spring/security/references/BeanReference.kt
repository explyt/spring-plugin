package com.esprito.spring.security.references

import com.esprito.spring.core.service.SpringSearchServiceFacade
import com.esprito.spring.security.SpringIcons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult

class BeanReference(
    element: PsiElement,
    range: TextRange,
    private val beanName: String
) : PsiReferenceBase.Poly<PsiElement>(element, range, false) {
    override fun resolve(): PsiElement? {
        val resolveResults: Array<out ResolveResult> = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val springSearchService = SpringSearchServiceFacade.getInstance(element.project)
        val beanPsiClass = springSearchService.getAllBeanByNames(module).asSequence()
            .filter { it.key == beanName }
            .flatMap { it.value.map { psiBean -> psiBean.psiMember } }
            .toList()

        return beanPsiClass.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val allBeans = SpringSearchServiceFacade.getInstance(myElement.project).getAllActiveBeans(module)
        return allBeans.map { bean ->
            LookupElementBuilder.create(bean.name)
                .withIcon(SpringIcons.SpringBean)
                .withTypeText(bean.psiClass.containingFile?.name)
        }.toTypedArray()
    }

}