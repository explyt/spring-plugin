package com.esprito.spring.core.completion.properties

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

abstract class AbstractSpringMetadataConfigurationPropertiesLoader(project: Project) : ConfigurationPropertiesLoader {

    protected val psiManager = PsiManager.getInstance(project)

    private val mapper = ObjectMapper(JsonFactory()).apply {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    companion object {
        const val CONFIGURATION_METADATA_FILE_NAME = "spring-configuration-metadata.json"
        const val ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME = "additional-spring-configuration-metadata.json"
        const val SPRING_FACTORIES_FILE_NAME = "spring.factories"

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
            val propertyName = it.name
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

    protected fun collectConfigurationProperties(
        metaDataFileText: String,
        metaDataFilePath: String,
        configurationProperties: MutableMap<String, ConfigurationProperty>
    ) {
        val metadata = loadMetadata(metaDataFileText, metaDataFilePath, SpringConfigurationPropertiesMetadata::class.java)
            ?: return

        metadata.properties?.forEach {
            val propertyName = it.name
            val existProperty = configurationProperties[propertyName]
            if (existProperty == null) {
                configurationProperties[propertyName] = ConfigurationProperty(
                    name = propertyName,
                    type = it.type,
                    sourceType = it.sourceType,
                    description = it.description,
                    defaultValue = it.defaultValue
                )
            } else {
                if (existProperty.type.isNullOrEmpty()) {
                    existProperty.type = it.type
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
            }
        }
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
    val name: String,
    @JsonProperty("type")
    val type: String?,
    @JsonProperty("description")
    val description: String?,
    @JsonProperty("deprecated")
    val deprecated: Boolean?,
    @JsonProperty("sourceType")
    val sourceType: String?,
    @JsonProperty("defaultValue")
    val defaultValue: Any?
)

class SpringConfigurationMetadataHint @JsonCreator constructor(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("values")
    val values: List<ValueHint>?,
    @JsonProperty("providers")
    val providers: List<ProviderHint>?
)

class ProviderHint @JsonCreator constructor(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("parameters")
    val parameters: ProviderParameters?
)

class ProviderParameters @JsonCreator constructor(
    @JsonProperty("target")
    val target: String?
)

class ValueHint @JsonCreator constructor(
    @JsonProperty("value")
    val value: String,
    @JsonProperty("description")
    val description: String?
)