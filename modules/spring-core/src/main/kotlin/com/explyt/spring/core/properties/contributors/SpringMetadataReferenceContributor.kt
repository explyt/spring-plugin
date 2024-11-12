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

package com.explyt.spring.core.properties.contributors

import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.SpringProperties.NAME
import com.explyt.spring.core.SpringProperties.POSTFIX_KEYS
import com.explyt.spring.core.SpringProperties.POSTFIX_VALUES
import com.explyt.spring.core.SpringProperties.SOURCE_TYPE
import com.explyt.spring.core.SpringProperties.TARGET
import com.explyt.spring.core.SpringProperties.TYPE
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.core.properties.references.SpringMetadataPropertyNameReference
import com.explyt.spring.core.properties.references.SpringMetadataValueProviderReference
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.json.psi.*
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiFilePattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.util.ProcessingContext

class SpringMetadataReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {

        val originalClassReferenceProvider = JavaClassReferenceProvider().apply {
            setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, true)
        }

        val javaClassReferenceProvider = getJavaClassReferenceProvider(originalClassReferenceProvider)

        // -- type or sourceType
        registrar.registerReferenceProvider(
            ((PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(StandardPatterns.or(additionalConfigMetadata, springConfigMetadata)))
                .with(propertyValue))
                .withParent(
                    PlatformPatterns.psiElement(JsonProperty::class.java)
                        .with(classPropertyNames)
                ),
            javaClassReferenceProvider
        )

        // -- hints.providers.parameters.target
        registrar.registerReferenceProvider(
            (((PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(StandardPatterns.or(additionalConfigMetadata, springConfigMetadata)))
                .with(propertyValue))
                .withParent(
                    PlatformPatterns.psiElement(JsonProperty::class.java)
                        .with(targetProperty)
                ))
                .withSuperParent(3, groupContextWithObject),
            javaClassReferenceProvider
        )

        // -- hints.name
        registrar.registerReferenceProvider(
            ((((PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(additionalConfigMetadata))
                .with(propertyValue))
                .withParent(
                    PlatformPatterns.psiElement(JsonProperty::class.java)
                        .with(propertyName)
                ))
                .withSuperParent(4, groupContextWithArray))
                .with(hintsGroup),
            getHintNameReferenceProvider()
        )

        // -- hints.providers
        registrar.registerReferenceProvider(
            ((((PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(additionalConfigMetadata))
                .with(propertyValue))
                .withParent(
                    PlatformPatterns.psiElement(JsonProperty::class.java)
                        .with(propertyName)
                ))
                .withSuperParent(4, groupContextWithArray))
                .with(hintsProvidersGroup),
            getHintProvidersReferenceProvider()
        )
    }

    private fun getHintNameReferenceProvider(): PsiReferenceProvider =
        object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
                val property = ElementManipulators.getValueText(element)
                val prefixLength = getPrefixLength(property)
                val textRange = if (prefixLength != -1) TextRange.from(1, prefixLength) else null
                val propertyKey = if (prefixLength != -1) property.substring(0, prefixLength) else property
                return arrayOf(ConfigurationPropertyKeyReference(element, module, propertyKey, textRange, HINT))
            }

            private fun getPrefixLength(property: String): Int {
                var prefixLength = -1
                if (property.endsWith(POSTFIX_KEYS) || property.endsWith(POSTFIX_VALUES)) {
                    prefixLength = property.lastIndexOf(POSTFIX_KEYS)
                    if (prefixLength == -1) {
                        prefixLength = property.lastIndexOf(POSTFIX_VALUES)
                    }
                }
                return prefixLength
            }
        }

    private fun getHintProvidersReferenceProvider(): PsiReferenceProvider =
        object : PsiReferenceProvider() {
            override fun getReferencesByElement(
                element: PsiElement,
                context: ProcessingContext
            ): Array<PsiReference> {
                return arrayOf(SpringMetadataValueProviderReference(element))
            }
        }

    private fun getJavaClassReferenceProvider(originalProvider: JavaClassReferenceProvider): PsiReferenceProvider =
        object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                val javaClassReference = object : JavaClassReferenceSet(
                    ElementManipulators.getValueText(element),
                    element,
                    ElementManipulators.getOffsetInElement(element),
                    false,
                    originalProvider
                ) {
                    override fun isAllowDollarInNames(): Boolean = true
                }
                return javaClassReference.references
            }
        }

    private val groupContextKey: Key<SpringMetadataPropertyNameReference.GroupContext> = Key.create(GROUP_CONTEXT)

    private val groupContext = object : PatternCondition<JsonProperty>(GROUP_CONTEXT) {
        override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext): Boolean {
            val propertyName = jsonProperty.name
            val groupContext = SpringMetadataPropertyNameReference.GroupContext.forProperty(propertyName)
                ?: return false
            context.put(groupContextKey, groupContext)
            return true
        }
    }

    private val groupContextWithObject =
        (PlatformPatterns.psiElement(JsonProperty::class.java)
            .with(groupContext))
            .with(object : PatternCondition<JsonProperty>(JSON_OBJECT_VALUE) {
                override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext) =
                    jsonProperty.value is JsonObject
            })

    private val groupContextWithArray =
        (PlatformPatterns.psiElement(JsonProperty::class.java)
            .with(groupContext))
            .with(object : PatternCondition<JsonProperty>(JSON_ARRAY_VALUE) {
                override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext) =
                    jsonProperty.value is JsonArray
            })

    private val additionalConfigMetadata =
        PlatformPatterns.psiFile(JsonFile::class.java)
            .with(object : PatternCondition<JsonFile>(IS_ADDITIONAL_JSON) {
                override fun accepts(jsonFile: JsonFile, context: ProcessingContext) =
                    SpringCoreUtil.isAdditionalConfigFile(jsonFile)
            })

    private val springConfigMetadata = PlatformPatterns.psiFile(JsonFile::class.java)
        .withName(SpringProperties.CONFIGURATION_METADATA_FILE_NAME) as PsiFilePattern.Capture<JsonFile>


    private val propertyValue = object : PatternCondition<JsonStringLiteral>(IS_PROPERTY_VALUE) {
        override fun accepts(property: JsonStringLiteral, context: ProcessingContext) =
            JsonPsiUtil.isPropertyValue(property)
    }

    private val propertyName = object : PatternCondition<JsonProperty>(NAME_PROPERTY) {
        override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext) =
            NAME == jsonProperty.name
    }

    private val classPropertyNames = object : PatternCondition<JsonProperty>(CLASS_PROPERTY_NAMES) {
        override fun accepts(property: JsonProperty, context: ProcessingContext) =
            TYPE == property.name || SOURCE_TYPE == property.name
    }

    private val targetProperty = object : PatternCondition<JsonProperty>(TARGET_PROPERTY) {
        override fun accepts(property: JsonProperty, context: ProcessingContext) =
            TARGET == property.name
    }

    private val hintsGroup = object : PatternCondition<JsonStringLiteral>(HINTS_GROUP) {
        override fun accepts(literal: JsonStringLiteral, context: ProcessingContext) =
            context.get(groupContextKey).equals(SpringMetadataPropertyNameReference.GroupContext.HINTS)
    }

    private val hintsProvidersGroup = object : PatternCondition<JsonStringLiteral>(HINTS_PROVIDERS_GROUP) {
        override fun accepts(literal: JsonStringLiteral, context: ProcessingContext) =
            context.get(groupContextKey).equals(SpringMetadataPropertyNameReference.GroupContext.HINTS_PROVIDERS)
    }

    companion object {
        const val HINT = "Hint"

        const val GROUP_CONTEXT = "groupContext"
        const val JSON_OBJECT_VALUE = "jsonObjectValue"
        const val JSON_ARRAY_VALUE = "jsonArrayValue"

        const val NAME_PROPERTY = "nameProperty"
        const val CLASS_PROPERTY_NAMES = "classPropertyNames"
        const val TARGET_PROPERTY = "targetProperty"
        const val HINTS_GROUP = "hintsGroup"
        const val HINTS_PROVIDERS_GROUP = "hintsProvidersGroup"

        const val IS_ADDITIONAL_JSON = "isAdditionalJson"
        const val IS_PROPERTY_VALUE = "inPropertyValue"
    }

}