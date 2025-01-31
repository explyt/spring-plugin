/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.properties.providers

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringProperties.COLON
import com.explyt.spring.core.SpringProperties.HINTS
import com.explyt.spring.core.SpringProperties.LOGGING_LEVEL
import com.explyt.spring.core.SpringProperties.NAME
import com.explyt.spring.core.SpringProperties.POSTFIX_KEYS
import com.explyt.spring.core.SpringProperties.POSTFIX_VALUES
import com.explyt.spring.core.completion.properties.*
import com.explyt.spring.core.properties.PropertiesJavaClassReferenceSet
import com.explyt.spring.core.properties.references.MetaConfigurationKeyReference
import com.explyt.spring.core.properties.references.PropertiesKeyMapValueReference
import com.explyt.spring.core.properties.references.YamlKeyMapValueReference
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticInsertHandler
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.DOT
import com.explyt.spring.core.util.PropertyUtil.propertyKey
import com.explyt.spring.core.util.PropertyUtil.toBooleanAlias
import com.explyt.spring.core.util.RenameUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.JsonUtil
import com.intellij.json.psi.*
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.yaml.YAMLLanguage

class SpringConfigurationPropertyKeyReferenceProvider : PsiReferenceProvider() {

    private val referenceProvider = JavaClassReferenceProvider()
        .apply { isSoft = true }

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return emptyArray()
        }
        val propertyKey = element.propertyKey() ?: return PsiReference.EMPTY_ARRAY
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return PsiReference.EMPTY_ARRAY
        val allHints = SpringConfigurationPropertiesSearch.getInstance(element.project).getAllHints(module)

        val keyHint: PropertyHint? = allHints.find { hint ->
            val hintName = hint.name
            val keysIdx = hintName.lastIndexOf(POSTFIX_KEYS)
            if (keysIdx == -1) {
                return@find false
            }
            propertyKey.startsWith(hintName.substring(0, keysIdx))
        }
        if (keyHint == null) {
            return if (isMapValue(module, propertyKey)) {
                getMapValueReferences(element, module, propertyKey)
            } else {
                arrayOf(
                    ConfigurationPropertyKeyReference(element, module, propertyKey),
                    MetaConfigurationKeyReference(element, module, propertyKey)
                )
            }
        }

        val referencesByPrefixKey = getPsiReferencesByPrefixKeys(propertyKey, module, keyHint, element)
        if (referencesByPrefixKey.isNotEmpty()) {
            return referencesByPrefixKey
        }
        val referencesByLoggingLevel = getPsiReferencesByMapKeyLoggingLevel(propertyKey, element)
        if (referencesByLoggingLevel.isNotEmpty()) {
            return referencesByLoggingLevel
        }

        return arrayOf(ConfigurationPropertyKeyReference(element, module, propertyKey))
    }

    private fun getMapValueReferences(
        element: PsiElement,
        module: Module,
        propertyKey: String
    ): Array<PsiReference> {
        val prefixes = generatePrefixes(propertyKey.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED))
        val properties = SpringConfigurationPropertiesSearch.getInstance(element.project)
            .getAllProperties(module)
        val foundProperty = properties.asSequence()
            .filter { property -> prefixes.any { prefix -> property.isMap() && property.name == prefix } }
            .firstOrNull() ?: return PsiReference.EMPTY_ARRAY

        return when (element.language) {
            YAMLLanguage.INSTANCE -> getMapValueReferencesForYaml(module, element, propertyKey, foundProperty.name)
            PropertiesLanguage.INSTANCE -> getMapValueReferencesForProperties(
                module,
                element,
                propertyKey,
                foundProperty.type
            )

            else -> emptyArray()
        }
    }

    private fun getMapValueReferencesForYaml(
        module: Module,
        element: PsiElement,
        propertyKey: String,
        propertyName: String
    ): Array<PsiReference> {
        val references = mutableListOf<PsiReference>()

        val propertyKeyPath = propertyKey.substringAfter("$propertyName.").split(".")
        val elementText = element.text

        var currentOffset = 0
        propertyKeyPath.forEach { key ->
            val keyIndex = elementText.indexOf(key, currentOffset)
            if (keyIndex != -1) {
                val startOffset = keyIndex
                val endOffset = startOffset + key.length

                val textRange = TextRange(startOffset, endOffset)
                references.add(YamlKeyMapValueReference(element, module, propertyKey, textRange))

                currentOffset = endOffset
            }
        }
        return references.toTypedArray()
    }

    private fun getMapValueReferencesForProperties(
        module: Module,
        element: PsiElement,
        propertyKey: String,
        propertyType: String?
    ): Array<PsiReference> {
        val project = module.project
        val valueType = PropertyUtil.getValueClassNameInMap(propertyType) ?: return emptyArray()
        val qualifiedName = valueType.substringBeforeLast('#').replace('$', '.')
        val foundClass =
            JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))
                ?: return emptyArray()
        if (foundClass.qualifiedName?.startsWith("java.lang") == true) return PsiReference.EMPTY_ARRAY

        val results = hashMapOf<String, ConfigurationProperty>()

        PropertyUtil.collectConfigurationProperty(module, foundClass, foundClass, "", results)
        val startOffset = propertyKey
            .substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
            .substringBeforeLast(".")
            .length + 1

        return results.values.distinctBy { it.name }.mapNotNull {
            val value = propertyKey.substringAfterLast(".")
            val textRange = TextRange(startOffset, startOffset + value.length)
            if (element.textLength >= textRange.endOffset) {
                PropertiesKeyMapValueReference(element, module, textRange, it)
            } else {
                null
            }
        }.toTypedArray()
    }

    private fun getPsiReferencesByPrefixKeys(
        propertyKey: String,
        module: Module,
        keyHint: PropertyHint,
        element: PsiElement
    ): Array<PsiReference> {
        if (element.language == YAMLLanguage.INSTANCE) return emptyArray()

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
                    module,
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

        return arrayOf(ConfigurationPropertyKeyReference(element, module, propertyKey))
    }

    private fun getPsiReferencesByMapKeyLoggingLevel(
        propertyKey: String,
        element: PsiElement
    ): Array<PsiReference> {
        if (!propertyKey.startsWith("$LOGGING_LEVEL.")) return emptyArray()

        val valueText = ElementManipulators.getValueText(element)
        val offset = ElementManipulators.getOffsetInElement(element)

        return JavaClassReferenceSet(valueText, element, offset, false, referenceProvider)
            .references
    }

    private fun isMapValue(
        module: Module,
        propertyKey: String
    ): Boolean {
        val prefixes = generatePrefixes(propertyKey.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED))
        val project = module.project
        val properties = SpringConfigurationPropertiesSearch.getInstance(project)
            .getAllProperties(module)
        if (properties.any { it.name == propertyKey }) return false

        val property = properties.asSequence()
            .filter { property -> prefixes.any { prefix -> property.isMap() && property.name == prefix } }
            .firstOrNull() ?: return false
        return generatePrefixes(property.name).size + 2 == prefixes.size
    }

    private fun generatePrefixes(key: String): List<String> {
        val parts = key.split(".")
        val prefixes = mutableListOf<String>()
        for (i in parts.indices) {
            prefixes.add(parts.take(parts.size - i).joinToString("."))
        }
        return prefixes
    }

}

class ConfigurationPropertyKeyReference(
    element: PsiElement,
    module: Module,
    private val propertyKey: String,
    textRange: TextRange? = null,
    private val mode: String? = null,
) : MetaConfigurationKeyReference(element, module, propertyKey, textRange), EmptyResolveMessageProvider {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val results = resultConfigKeyPsiElement(project, module)
        if (results.isNotEmpty()) {
            return results
        }
        return super.multiResolve(incompleteCode)
    }

    private fun resultConfigKeyPsiElement(project: Project, module: Module): Array<ResolveResult> {
        val foundProperty = PropertyUtil.configurationProperty(project, module, propertyKey) ?: return emptyArray()

        val sourceType = when {
            foundProperty.propertyType == PropertyType.MAP -> {
                return handleMapProperty(project, foundProperty)
            }

            PropertyUtil.isSameProperty(foundProperty.name, propertyKey) -> {
                foundProperty.sourceType ?: return emptyArray()
            }

            foundProperty.isBooleanType() -> {
                resolveBooleanSourceType(foundProperty) ?: return emptyArray()
            }

            else -> return emptyArray()
        }

        val sourceMember = PropertyUtil.findSourceMember(propertyKey, sourceType, project) ?: return emptyArray()
        return resolveResults(sourceMember)
    }

    private fun handleMapProperty(project: Project, foundProperty: ConfigurationProperty): Array<ResolveResult> {
        if (foundProperty.name == propertyKey) {
            return resolveResultsByProperty(foundProperty, project)
        }

        val valueType = PropertyUtil.getValueClassNameInMap(foundProperty.type) ?: return emptyArray()
        val propertyMapKey = propertyKey.substringAfter("${foundProperty.name}.").substringBefore(".")
        var propertyMapValue = propertyKey.substringAfter("$propertyMapKey.")
        if (propertyMapValue == propertyKey) propertyMapValue = ""

        return if (propertyMapKey.isNotEmpty() && propertyMapValue.isEmpty()) {
            val source = PropertyUtil.findSourceMember("", valueType, project) ?: return emptyArray()
            resolveResults(source)
        } else {
            val methods = PropertyUtil.getMethodsTypeByMap(module, valueType, propertyMapValue)
                .filter { PropertyUtil.isNameSetMethod(it.name, propertyMapValue) }
                .ifEmpty { return emptyArray() }
            resolveResults(methods.first())
        }
    }

    private fun resolveResultsByProperty(
        foundProperty: ConfigurationProperty,
        project: Project
    ): Array<ResolveResult> {
        val type = foundProperty.sourceType ?: return emptyArray()
        val source = PropertyUtil.findSourceMember(propertyKey, type, project) ?: return emptyArray()
        return resolveResults(source)
    }

    private fun resolveBooleanSourceType(foundProperty: ConfigurationProperty): String? {
        if (toBooleanAlias(foundProperty.name, foundProperty.type)
            == toBooleanAlias(propertyKey, foundProperty.type)
        ) {
            return foundProperty.sourceType
        }
        return null
    }

    private fun resolveResults(sourceMember: PsiMember): Array<ResolveResult> {
        val uElement = sourceMember.toUElement() ?: return emptyArray()

        return if ((uElement is UClass || uElement is UMethod || uElement is UField || !mode.isNullOrEmpty())) {
            PsiElementResolveResult.createResults(ConfigKeyPsiElement(sourceMember))
        } else {
            emptyArray()
        }
    }

    override fun getVariants(): Array<Any> {
        if (mode == null) return emptyArray()

        val project = module.project
        val properties = SpringConfigurationPropertiesSearch.getInstance(project)
            .getAllProperties(module)

        val existingKeys = if (mode == HINTS) loadExistingNameKeys() else emptySet()
        return properties
            .flatMap { generateVariantsByMetadataHints(existingKeys, it) }
            .toTypedArray()
    }

    private fun generateVariantsByMetadataHints(
        existingKeys: Set<String>,
        property: ConfigurationProperty
    ): List<LookupElement> {
        return if (property.isMap()) {
            listOfNotNull(
                createVariant(existingKeys, property.name + POSTFIX_KEYS, property),
                createVariant(existingKeys, property.name + POSTFIX_VALUES, property)
            )
        } else {
            listOfNotNull(createVariant(existingKeys, property.name, property))
        }
    }

    private fun createVariant(
        existingKeys: Set<String>,
        keysName: String,
        property: ConfigurationProperty
    ): LookupElement? {
        if (existingKeys.contains(keysName)) return null

        return LookupElementBuilder.create(property, keysName)
                .withRenderer(PropertyRenderer())
                .withInsertHandler(StatisticInsertHandler(StatisticActionId.COMPLETION_PROPERTY_KEY_CONFIGURATION))
    }

    private fun loadExistingNameKeys(): Set<String> {
        val file = element.containingFile as? JsonFile ?: return emptySet()

        val rootObject = file.topLevelValue as? JsonObject ?: return emptySet()
        val hintsArray = JsonUtil.getPropertyValueOfType(rootObject, HINTS, JsonArray::class.java) ?: return emptySet()

        val result = HashSet<String>()
        for (value in hintsArray.valueList) {
            if (value is JsonValue) {
                val entry = value as? JsonObject ?: continue
                val literal = JsonUtil.getPropertyValueOfType(entry, NAME, JsonStringLiteral::class.java) ?: continue

                result.add(literal.value)
            }
        }
        return result
    }

    override fun getUnresolvedMessagePattern(): String {
        return SpringCoreBundle.message(
            "explyt.spring.inspection.metadata.config.unresolved.key.reference",
            this.value
        )
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return super.handleElementRename(newElementName)
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

    override fun toString(): String {
        return member.name ?: "Unnamed"
    }

    override fun getText(): String? {
        return member.text
    }

    override fun getName(): String? {
        return member.name
    }

    override fun setName(name: String): PsiElement {
        if (member !is PsiMethod) return member
        val result = PropertyUtil.findPropertyByConfigurationPropertyElement(member) ?: return rename(name)

        val newPropertyName = RenameUtil.convertSetterToPKebabCase(name.substringAfter("set"))
        val prefix = RenameUtil.convertSetterToPKebabCase(result.prefix)
        result.properties
            .forEach { RenameUtil.renameProperty(it, newPropertyName, prefix, true) }

        return rename(name)
    }

    private fun rename(newName: String): PsiElement {
        if (member.language == KotlinLanguage.INSTANCE) {
            val name = newName.substringAfter("set").replaceFirstChar { it.lowercase() }
            (member.toUElement()?.sourcePsi as? KtParameter)?.setName(name)
        } else {
            (member as? PsiNamedElement)?.setName(newName)
        }

        return member
    }

}