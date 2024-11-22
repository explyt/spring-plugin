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
import com.explyt.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.explyt.spring.core.properties.providers.metadata.SpringMetadataHintsNameReferenceProvider
import com.explyt.spring.core.properties.providers.metadata.SpringMetadataHintsProvidersNameReferenceProvider
import com.explyt.spring.core.properties.providers.metadata.SpringMetadataTypeReferenceProvider
import com.intellij.json.psi.*
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiFilePattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext


class SpringConfigurationMetadataReferenceContributor : PsiReferenceContributor() {

    private val keyTypes by lazy { arrayOf("type", "sourceType") }
    private val hintsNames by lazy { arrayOf("class-reference", "handle-as", "spring-bean-reference") }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral::class.java)
                .inFile(additionalFilePattern())
                .withSuperParent(2, JsonObject::class.java)
                .with(object : PatternCondition<JsonStringLiteral>("isTypePropertyValue") {
                    override fun accepts(literal: JsonStringLiteral, context: ProcessingContext): Boolean {
                        val jsonProperty = literal.parent as? JsonProperty
                        return jsonProperty?.name in keyTypes
                    }
                }),
            SpringMetadataTypeReferenceProvider()
        )

        registrar.registerReferenceProvider(
            createHintsNamePattern(),
            SpringMetadataHintsNameReferenceProvider()
        )

        registrar.registerReferenceProvider(
            createHintsProvidersNamePattern(),
            SpringMetadataHintsProvidersNameReferenceProvider()
        )

        registrar.registerReferenceProvider(
            createHintsProvidersTargetPattern(),
            SpringMetadataTypeReferenceProvider()
        )

    }

    private fun createHintsNamePattern() = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
        .inFile(additionalFilePattern())
        .withSuperParent(2, JsonObject::class.java)
        .withSuperParent(3, JsonArray::class.java)
        .withSuperParent(4, JsonProperty::class.java)
        .with(object : PatternCondition<JsonStringLiteral>("inHintsName") {
            override fun accepts(element: JsonStringLiteral, context: ProcessingContext): Boolean {
                return isHintsNameProperty(element, 1)
            }
        })

    private fun createHintsProvidersNamePattern() = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
        .inFile(additionalFilePattern())
        .withSuperParent(2, JsonObject::class.java)
        .withSuperParent(3, JsonArray::class.java)
        .withSuperParent(4, JsonProperty::class.java)
        .withSuperParent(5, JsonObject::class.java)
        .withSuperParent(6, JsonArray::class.java)
        .withSuperParent(7, JsonProperty::class.java)
        .with(object : PatternCondition<JsonStringLiteral>("inHintsProvidersName") {
            override fun accepts(element: JsonStringLiteral, context: ProcessingContext): Boolean {
                return isHintsNameProperty(element, 2)
            }
        })

    private fun createHintsProvidersTargetPattern() = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
        .inFile(additionalFilePattern())
        .withSuperParent(2, JsonObject::class.java)
        .withSuperParent(3, JsonProperty::class.java)
        .withSuperParent(4, JsonObject::class.java)
        .withSuperParent(5, JsonArray::class.java)
        .withSuperParent(6, JsonProperty::class.java)
        .withSuperParent(7, JsonObject::class.java)
        .withSuperParent(8, JsonArray::class.java)
        .withSuperParent(9, JsonProperty::class.java)
        .with(object : PatternCondition<JsonStringLiteral>("inHintsProvidersTarget") {
            override fun accepts(element: JsonStringLiteral, context: ProcessingContext): Boolean {
                return isHintsTargetProperty(element)
            }
        })

    private fun additionalFilePattern(): PsiFilePattern.Capture<JsonFile> {
        return PlatformPatterns.psiFile(JsonFile::class.java)
            .withName(ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME)
    }

    private fun isHintsNameProperty(element: JsonStringLiteral, elementLevel: Int): Boolean {
        val property = element.parent as? JsonProperty ?: return false
        if (SpringProperties.NAME != property.name) return false

        val hintsProperty = getHitsProperty(property) ?: return false
        if (elementLevel == 1 && SpringProperties.HINTS == hintsProperty.name) return true

        val parentObject = property.parent as? JsonObject ?: return false
        val providersArray = parentObject.parent as? JsonArray ?: return false
        val providersProperty = providersArray.parent as? JsonProperty ?: return false
        val hintsProvidersProperty = getHitsProperty(providersProperty) ?: return false
        return elementLevel == 2 && SpringProperties.HINTS == hintsProvidersProperty.name
    }

    private fun isHintsTargetProperty(element: JsonStringLiteral): Boolean {
        val property = element.parent as? JsonProperty ?: return false
        if (SpringProperties.TARGET != property.name) return false

        val parentObject = property.parent as? JsonObject ?: return false
        val parentProperty = parentObject.parent as? JsonProperty ?: return false

        val targetName = ((parentProperty.parent as? JsonObject)
            ?.findProperty(SpringProperties.NAME)
            ?.value as? JsonStringLiteral)?.value

        if (!hintsNames.contains(targetName)) return false

        val providersProperty = getHitsProperty(parentProperty) ?: return false
        val hintsProperty = getHitsProperty(providersProperty) ?: return false
        if (SpringProperties.HINTS == hintsProperty.name) return true
        return true
    }

    private fun getHitsProperty(property: PsiElement): JsonProperty? {
        val propertyObject = property.parent as? JsonObject ?: return null
        val propertyArray = propertyObject.parent as? JsonArray ?: return null
        return propertyArray.parent as? JsonProperty
    }

}


