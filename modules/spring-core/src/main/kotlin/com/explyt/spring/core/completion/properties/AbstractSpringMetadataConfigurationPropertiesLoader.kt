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

package com.explyt.spring.core.completion.properties

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringProperties.HINTS
import com.explyt.spring.core.SpringProperties.LOGGING_LEVEL
import com.explyt.spring.core.SpringProperties.NAME
import com.explyt.spring.core.SpringProperties.VALUE
import com.explyt.spring.core.SpringProperties.VALUES
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.json.JsonUtil
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager

abstract class AbstractSpringMetadataConfigurationPropertiesLoader(project: Project) : ConfigurationPropertiesLoader {

    protected val psiManager = PsiManager.getInstance(project)

    private val mapper = ObjectMapper(JsonFactory()).apply {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    companion object {
        private val logger = logger<AbstractSpringMetadataConfigurationPropertiesLoader>()
    }

    protected fun collectPropertyHints(
        metaDataFileText: String,
        metaDataFilePath: String
    ): List<PropertyHint> {
        val metadata = loadMetadata(metaDataFileText, metaDataFilePath, SpringConfigurationHintsMetadata::class.java)
            ?: return emptyList()

        val processedPropertyKeys = mutableSetOf<String>()
        return metadata.hints?.mapNotNull {
            val propertyName = it.name ?: return emptyList()
            if (processedPropertyKeys.add(propertyName)) {
                PropertyHint(
                    name = propertyName,
                    values = it.values ?: emptyList(),
                    providers = it.providers ?: emptyList()
                )
            } else {
                null
            }
        } ?: emptyList()
    }

    protected fun collectElementMetadataName(file: JsonFile, name: String): List<ElementHint> {
        val topValue = file.topLevelValue as? JsonObject ?: return emptyList()
        val jsonElement = JsonUtil.getPropertyValueOfType(topValue, name, JsonArray::class.java) ?: return emptyList()

        return jsonElement.valueList
            .asSequence()
            .mapNotNull { it as? JsonObject }
            .mapNotNull { it.findProperty(NAME) }
            .filter { it.value is JsonStringLiteral && it.value != null }
            .map { ElementHint((it.value as JsonStringLiteral).value, it) }
            .toList()
    }

    protected fun collectElementMetadataHintsValue(
        file: JsonFile,
        propertyName: String,
        valueName: String
    ): ElementHint? {
        val topValue = file.topLevelValue as? JsonObject ?: return null
        val jsonElement = JsonUtil.getPropertyValueOfType(topValue, HINTS, JsonArray::class.java) ?: return null

        val values = jsonElement.valueList.asSequence()
            .mapNotNull { it as? JsonObject }
            .firstOrNull {
                it.findProperty(NAME)?.value?.text == "\"$propertyName\""
                        || it.findProperty(NAME)?.value?.text == "\"${propertyName.substringBeforeLast(".")}.$VALUES\""
                        || it.findProperty(NAME)?.value?.text == "\"$LOGGING_LEVEL.$VALUES\""
            }
            ?.findProperty(VALUES)?.value as? JsonArray ?: return null

        val value = values.valueList.asSequence()
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it.findProperty(VALUE)?.value?.text == "\"$valueName\"" }
            ?.propertyList?.firstOrNull { it.name == VALUE } ?: return null

        return ElementHint((value.value as JsonStringLiteral).value, value)
    }

    protected fun collectConfigurationProperties(
        project: Project,
        metaDataFileText: String,
        metaDataFilePath: String,
        configurationProperties: MutableMap<String, ConfigurationProperty>
    ) {
        val metadata =
            loadMetadata(metaDataFileText, metaDataFilePath, SpringConfigurationPropertiesMetadata::class.java)
                ?: return

        for (it in metadata.properties ?: emptyList()) {
            val propertyName = it.name ?: continue

            val existProperty = configurationProperties[propertyName]
            val psiClass = getPsiClass(project, it.type)
            if (existProperty == null) {
                configurationProperties[propertyName] = ConfigurationProperty(
                    name = propertyName,
                    type = it.type,
                    propertyType = ConfigurationPropertiesLoader.getPropertyType(psiClass, it.type),
                    sourceType = it.sourceType,
                    description = it.description,
                    defaultValue = it.defaultValue,
                    deprecation = deprecationInfo(it.deprecation)
                )
            } else {
                if (existProperty.type.isNullOrEmpty()) {
                    existProperty.type = it.type
                }
                if (existProperty.propertyType == null) {
                    existProperty.propertyType = ConfigurationPropertiesLoader.getPropertyType(psiClass, it.type)
                }
                if (existProperty.sourceType.isNullOrEmpty()) {
                    existProperty.sourceType = it.sourceType
                }
                if (existProperty.description.isNullOrEmpty()) {
                    existProperty.description = it.description
                }
                if (existProperty.defaultValue == null) {
                    existProperty.defaultValue = it.defaultValue
                }
                if (existProperty.deprecation == null) {
                    existProperty.deprecation = deprecationInfo(it.deprecation)
                }
            }
        }
    }

    private fun deprecationInfo(deprecation: SpringConfigurationMetadataDeprecation?): DeprecationInfo? {
        if (deprecation == null) return null

        val level = deprecation.level
        val deprecationLevel = if (level == null) {
            DeprecationInfoLevel.WARNING
        } else {
            DeprecationInfoLevel.valueOf(level.uppercase())
        }
        return DeprecationInfo(
            deprecationLevel,
            deprecation.replacement,
            deprecation.reason
        )
    }

    private fun <T> loadMetadata(
        metaDataFileText: String,
        metaDataFilePath: String,
        clazz: Class<T>
    ): T? {
        return try {
            mapper.readValue(
                metaDataFileText,
                clazz
            )
        } catch (e: Exception) {
            if (e !is ControlFlowException) {
                logger.warn(
                    "Exception during parsing spring configuration metadata json file: $metaDataFilePath", e
                )
            }
            null
        }
    }

    private fun getPsiClass(project: Project, fqName: String?): PsiClass? {
        val className = fqName?.substringBefore("<") ?: return null
        return LibraryClassCache.searchForLibraryClass(project, className)
    }
}


data class SpringConfigurationPropertiesMetadata @JsonCreator constructor(
    @JsonProperty("properties")
    val properties: List<SpringConfigurationMetadataProperty>? = mutableListOf(),
)

data class SpringConfigurationHintsMetadata @JsonCreator constructor(
    @JsonProperty("hints")
    val hints: List<SpringConfigurationMetadataHint>? = mutableListOf()
)

data class SpringConfigurationMetadataProperty @JsonCreator constructor(
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("type")
    val type: String?,
    @JsonProperty("description")
    val description: String?,
    @JsonProperty("sourceType")
    val sourceType: String?,
    @JsonProperty("defaultValue")
    val defaultValue: Any?,
    @JsonProperty("deprecation")
    val deprecation: SpringConfigurationMetadataDeprecation?
)

data class SpringConfigurationMetadataDeprecation @JsonCreator constructor(
    @JsonProperty("level")
    val level: String?,
    @JsonProperty("reason")
    val reason: String?,
    @JsonProperty("replacement")
    val replacement: String?,
)

class SpringConfigurationMetadataHint @JsonCreator constructor(
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("values")
    val values: List<ValueHint>?,
    @JsonProperty("providers")
    val providers: List<ProviderHint>?
)

class ProviderHint @JsonCreator constructor(
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("parameters")
    val parameters: ProviderParameters?
)

class ProviderParameters @JsonCreator constructor(
    @JsonProperty("target")
    val target: String?
)

class ValueHint @JsonCreator constructor(
    @JsonProperty("value")
    val value: String?,
    @JsonProperty("description")
    val description: String?
)