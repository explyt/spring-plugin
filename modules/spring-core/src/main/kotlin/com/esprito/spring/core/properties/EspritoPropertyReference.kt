package com.esprito.spring.core.properties

import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

class EspritoPropertyReference(
    element: PsiElement,
    private val propertyKey: String,
    rangeInElement: TextRange
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference, HighlightedReference {

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val configurationPropertiesSearch = DefinedConfigurationPropertiesSearch.getInstance(element.project)
        val foundProps = configurationPropertiesSearch.findProperties(module, propertyKey)
        return foundProps.mapNotNull { property ->
            property.psiElement?.let { PsiElementResolveResult(it) }
        }.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val resolveResults: Array<out ResolveResult> = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        val project: Project = myElement.project
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val allProperties = DefinedConfigurationPropertiesSearch.getInstance(project).getAllProperties(module)
        return allProperties.map { property ->
            val psiElement = property.psiElement
            LookupElementBuilder.create(property.key)
                .withIcon(AllIcons.Nodes.Property)
                .withTypeText(psiElement?.containingFile?.name)
        }.toTypedArray()
    }
}