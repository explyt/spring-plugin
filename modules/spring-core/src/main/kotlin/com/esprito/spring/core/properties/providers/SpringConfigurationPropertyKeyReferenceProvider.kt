package com.esprito.spring.core.properties.providers

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringProperties.COLON
import com.esprito.spring.core.SpringProperties.POSTFIX_KEYS
import com.esprito.spring.core.completion.properties.PropertyHint
import com.esprito.spring.core.completion.properties.PropertyRenderer
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.properties.PropertiesJavaClassReferenceSet
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.spring.core.util.PropertyUtil.DOT
import com.esprito.spring.core.util.PropertyUtil.propertyKey
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.FakePsiElement
import com.intellij.util.ProcessingContext

class SpringConfigurationPropertyKeyReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return emptyArray()
        }
        val propertyKey = element.propertyKey() ?: return PsiReference.EMPTY_ARRAY
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return PsiReference.EMPTY_ARRAY
        val allHints = SpringConfigurationPropertiesSearch.getInstance(element.project).getAllHints(module)

        val keyHint: PropertyHint = allHints.find { hint ->
            val hintName = hint.name
            val keysIdx = hintName.lastIndexOf(POSTFIX_KEYS)
            if (keysIdx == -1) {
                return@find false
            }
            propertyKey.startsWith(hintName.substring(0, keysIdx))
        } ?: return arrayOf(ConfigurationPropertyKeyReference(element, propertyKey))

        val referencesByPrefixKey = getPsiReferencesByPrefixKeys(propertyKey, keyHint, element)
        if (referencesByPrefixKey.isNotEmpty()) {
            return referencesByPrefixKey
        }

        return arrayOf(ConfigurationPropertyKeyReference(element, propertyKey))
    }

    private fun getPsiReferencesByPrefixKeys(
        propertyKey: String,
        keyHint: PropertyHint,
        element: PsiElement
    ): Array<PsiReference> {
        val prefix = keyHint.name.substringBefore(POSTFIX_KEYS)
        val suffixKey = prefix.substringAfterLast(DOT)
        val suffixElement = element.text.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
            .filter { !it.isWhitespace() }

        if ((propertyKey.startsWith("$prefix.")
                    ||
                    (propertyKey.startsWith(prefix)
                            && suffixKey == suffixElement.substringBeforeLast(COLON)
                            && suffixElement.endsWith(COLON)))
            && (keyHint.providers.asSequence()
                .filter { it.name != null }
                .any { it.name == "logger-name" } || keyHint.values.isNotEmpty())
        ) {
            val prefixLength = prefix.length

            val result = mutableListOf<PsiReference>(
                ConfigurationPropertyKeyReference(
                    element,
                    prefix,
                    TextRange.from(0, prefixLength)
                )
            )

            val references: Array<PsiReference> = if (keyHint.values.isEmpty()) {
                PropertiesJavaClassReferenceSet(
                    propertyKey.substringAfter("$prefix."),
                    element,
                    prefixLength + 1
                ).references
            } else emptyArray()

            result.addAll(references)
            return result.toTypedArray()
        }

        return arrayOf(ConfigurationPropertyKeyReference(element, propertyKey))
    }
}

open class ConfigurationPropertyKeyReference(
    element: PsiElement,
    private val propertyKey: String,
    textRange: TextRange? = null,
    private val mode: String? = null,
) : PsiReferenceBase.Poly<PsiElement>(element, textRange, false), EmptyResolveMessageProvider {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val project = element.project
        val foundProperty = SpringConfigurationPropertiesSearch.getInstance(project)
            .findProperty(module, propertyKey) ?: return emptyArray()
        val sourceType = foundProperty.sourceType ?: return emptyArray()
        val sourceMember = PropertyUtil.findSourceMember(propertyKey, sourceType, project)
        if (sourceMember != null) {
            return PsiElementResolveResult.createResults(ConfigKeyPsiElement(sourceMember))
        }
        return emptyArray()
    }

    override fun getVariants(): Array<Any> {
        if (mode == null) {
            return emptyArray()
        }
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val properties = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getAllProperties(module)

        val allVariants = mutableListOf<LookupElement>()

        for (property in properties) {
            allVariants.add(
                LookupElementBuilder.create(property, property.name)
                    .withRenderer(PropertyRenderer())
            )
        }
        return allVariants.toTypedArray()
    }

    override fun getUnresolvedMessagePattern(): String {
        return SpringCoreBundle.message(
            "esprito.spring.inspection.metadata.config.unresolved.key.reference",
            this.value
        )
    }

}

class ConfigKeyPsiElement(private val member: PsiMember) : FakePsiElement() {

    override fun getParent(): PsiElement = member

    override fun getNavigationElement(): PsiElement = member.navigationElement

    override fun navigate(requestFocus: Boolean) {
        member.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return member.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return member.canNavigateToSource()
    }

    override fun getPresentation(): ItemPresentation? {
        return member.presentation
    }

    override fun getPresentableText(): String? {
        return member.presentation?.presentableText
    }
}