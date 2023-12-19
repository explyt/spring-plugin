package com.esprito.spring.core.properties.providers

import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PropertyUtil

abstract class ConfigurationPropertiesDocumentationProvider<T : PsiElement> : JavaDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val psiElement = (element as? ConfigKeyPsiElement)?.parent ?: return null
        val extractedOriginalElement = extractOriginalElement(originalElement) ?: return null
        val generateDoc = super.generateDoc(psiElement, originalElement) ?: return null

        val sectionsIdx = generateDoc.indexOf(DocumentationMarkup.SECTIONS_START)
        if (sectionsIdx == -1 || generateDoc.contains(DocumentationMarkup.CONTENT_START)) {
            return generateDoc
        }

        val propertyDescription = getPropertyDescription(psiElement, extractedOriginalElement)
        if (propertyDescription.isNullOrEmpty()) {
            return generateDoc
        }

        return """${generateDoc.substring(0, sectionsIdx)}
${DocumentationMarkup.CONTENT_START}
<p>$propertyDescription</p>
${DocumentationMarkup.CONTENT_END}
${generateDoc.substring(sectionsIdx + DocumentationMarkup.SECTIONS_START.length)}"""
    }

    private fun getPropertyDescription(psiElement: PsiElement, extractedOriginalElement: T): String? {
        if (psiElement !is PsiMethod) {
            return null
        }

        val propertiesFile = extractedOriginalElement.containingFile
        if (!SpringCoreUtil.isConfigurationPropertyFile(propertiesFile)) {
            return null
        }

        if (!PropertyUtil.isSetterName(psiElement.name)) {
            return null
        }

        val module = ModuleUtilCore.findModuleForPsiElement(extractedOriginalElement) ?: return null
        val propertyFullKey = getPropertyFullKey(extractedOriginalElement)
        val configurationProperty = SpringConfigurationPropertiesSearch.getInstance(extractedOriginalElement.project)
            .findProperty(module, propertyFullKey) ?: return null
        return configurationProperty.description
    }

    abstract fun getPropertyFullKey(extractedOriginalElement: T): String

    protected abstract fun extractOriginalElement(originalElement: PsiElement?): T?
}
