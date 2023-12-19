package com.esprito.spring.core.properties.providers

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.completion.properties.ProviderHint
import com.esprito.spring.core.references.PrefixReference
import com.esprito.spring.core.references.PrefixReferenceType
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

class SpringConfigurationPropertiesValueResourceReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val propertyKey = element.parentOfType<PropertyImpl>()?.key ?: return emptyArray()
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return emptyArray()
        }
        return getResourceReferences(element, propertyKey)
    }

    private fun getResourceReferences(element: PsiElement, propertyKey: String): Array<PsiReference> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val propertyHint = PropertyUtil.getPropertyHint(module, propertyKey) ?: return emptyArray()

        return propertyHint.providers.asSequence()
            .flatMap { processProviderHints(element, it).asSequence() }
            .toList().toTypedArray()
    }

    private fun processProviderHints(element: PsiElement, provider: ProviderHint): Array<PsiReference> {
        val providerName = provider.name
        val targetClassFqn = provider.parameters?.target ?: return emptyArray()
        if (providerName == SpringProperties.HANDLE_AS && targetClassFqn == SpringCoreClasses.IO_RESOURCE) {
            return getResourceVariants(element)
        }
        return emptyArray()
    }

    private fun getResourceVariants(element: PsiElement): Array<PsiReference> {
        val text = element.parentOfType<PropertyImpl>()?.value?.substringBefore(DUMMY_IDENTIFIER_TRIMMED) ?: return emptyArray()
        val references = mutableListOf<PsiReference>()
        when {
            text.startsWith(SpringProperties.PREFIX_HTTP) ->
                references += PropertyUtil.getReferenceWithoutPrefix(text, element, emptyArray(), this)

            text.startsWith(SpringProperties.PREFIX_FILE) ->
                references += PropertyUtil.getReferenceByFilePrefix(text, element, emptyArray(), this)

            text.startsWith(SpringProperties.PREFIX_CLASSPATH) ->
                references += PropertyUtil.getReferenceByClasspathPrefix(
                    text,
                    SpringProperties.PREFIX_CLASSPATH,
                    element,
                    emptyArray(),
                    this
                )

            text.startsWith(SpringProperties.PREFIX_CLASSPATH_STAR) ->
                references += PropertyUtil.getReferenceByClasspathPrefix(
                    text,
                    SpringProperties.PREFIX_CLASSPATH_STAR,
                    element,
                    emptyArray(),
                    this
                )

            else -> {
                references += PrefixReference(element, PrefixReferenceType.FILE_PROPERTY)
                references += PropertyUtil.getReferenceWithoutPrefix(text, element, emptyArray(), this)
            }
        }

        return references.toTypedArray()
    }
}
