package com.esprito.spring.core.properties.references

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.properties.providers.SpringMetadataValueProvider
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class SpringMetadataValueProviderReference(
    element: PsiElement
) : PsiReferenceBase<PsiElement>(element, true), EmptyResolveMessageProvider {
    override fun getUnresolvedMessagePattern(): String {
        return SpringCoreBundle.message("explyt.spring.inspection.metadata.config.unresolved.provider", this.value)
    }

    override fun resolve(): PsiElement? {
        val valueProvider = getValueProvider()
        return if (valueProvider != null) element else null
    }

    override fun getVariants(): Array<LookupElementBuilder> {
        return SpringMetadataValueProvider.entries.map {
            LookupElementBuilder.create(it.id)
                .appendTailText(" (${it.description})", true)
        }.toTypedArray()
    }

    fun getValueProvider(): SpringMetadataValueProvider? {
        return SpringMetadataValueProvider.findById(value)
    }

}