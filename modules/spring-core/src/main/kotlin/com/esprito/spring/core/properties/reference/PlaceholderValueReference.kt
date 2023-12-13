package com.esprito.spring.core.properties.reference

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.references.PropertyReferenceBase
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class PlaceholderValueReference(
    val key: String,
    element: PsiElement,
    textRange: TextRange
) : PropertyReferenceBase(key, true, element, textRange),
    HighlightedReference {
    override fun getVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val properties = DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getAllProperties(module)

        val variants = mutableListOf<Any>()
        properties.forEach { it ->
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

    override fun getPropertiesFiles(): List<PropertiesFile> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        return DefinedConfigurationPropertiesSearch.getInstance(module.project).searchPropertyFiles(module)
            .asSequence()
            .filter { it is PropertiesFile }
            .mapTo(mutableListOf()) { it as PropertiesFile }
    }
}
