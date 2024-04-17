package com.esprito.spring.core.properties.references

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.esprito.spring.core.util.PropertyUtil
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.properties.IProperty
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

class PlaceholderValueReference(
    val key: String,
    element: PsiElement,
    textRange: TextRange
) : PsiReferenceBase.Poly<PsiElement>(element, textRange, false),
    HighlightedReference {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val properties = DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getAllProperties(module)

        val results = mutableListOf<ResolveResult>()
        properties.forEach {
            val property = it.psiElement
            if (property is IProperty) {
                val propertyKey = property.key ?: return@forEach
                if (PropertyUtil.isSameProperty(propertyKey, key)) {
                    results.add(PsiElementResolveResult(property))
                }
            }

            if (property is YAMLKeyValue) {
                val propertyKey = YAMLUtil.getConfigFullName(property)
                if (PropertyUtil.isSameProperty(propertyKey, key)) {
                    results.add(PsiElementResolveResult(property))
                }
            }
        }
        return results.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val properties = DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getAllProperties(module)

        val variants = mutableListOf<Any>()
        properties.forEach {
            val property = it.psiElement
            if (property is IProperty) {
                val key = property.key ?: return@forEach
                variants.add(
                    LookupElementBuilder
                        .create(key)
                        .withTypeText(property.containingFile.name, SpringIcons.PropertyKey, true)
                )
            }
        }
        return variants.toTypedArray()
    }

}
