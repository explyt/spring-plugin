package com.esprito.spring.core.properties.references

import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult

open class MetaConfigKeyReference(
    element: PsiElement,
    private val propertyKey: String,
    textRange: TextRange? = null
) : PsiReferenceBase.Poly<PsiElement>(element, textRange, false) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val metadataProperties = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getElementNameProperties(module)
        var result = metadataProperties
            .filter { it.name == propertyKey }
            .map { it.jsonProperty }

        result = getOnlySourceIfExist(result)
        return PsiElementResolveResult.createResults(result)
    }

    private fun getOnlySourceIfExist(result: List<JsonProperty>): List<JsonProperty> {
        if (result.size <= 1) {
            return result
        }

        val resultWithSources = mutableListOf<JsonProperty>()
        for (it in result) {
            val path = it.containingFile.containingDirectory.virtualFile.canonicalPath
            if (path != null && path.contains("sources")) {
                resultWithSources.add(it)
            }
        }
        if (resultWithSources.isNotEmpty()) {
            return resultWithSources
        }

        return result
    }

}