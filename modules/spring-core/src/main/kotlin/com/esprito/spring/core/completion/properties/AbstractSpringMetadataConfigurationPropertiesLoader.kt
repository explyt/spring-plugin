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

        private val logger = logger<AbstractSpringMetadataConfigurationPropertiesLoader>()
    }

    protected fun collectConfigurationProperties(
        metaDataFileText: String,
        metaDataFilePath: String
    ): List<ConfigurationProperty> {
        val metadata = try {
            mapper.readValue(
                metaDataFileText,
                SpringConfigurationMetadata::class.java
            )
        } catch (e: Exception) {
            if (e !is ControlFlowException) {
                logger.warn(
                    "Exception during parsing spring configuration metadata json file: ${metaDataFilePath}", e
                )
            }
            return emptyList()
        }

        val processedPropertyKeys = mutableSetOf<String>()
        return metadata.properties.mapNotNull {
            val propertyName = it.name
            if (processedPropertyKeys.add(propertyName)) {
                ConfigurationProperty(
                    name = propertyName,
                    type = it.type,
                    sourceType = it.sourceType,
                    description = it.description,
                    defaultValue = it.defaultValue
                )
            } else {
                null
            }
        }
    }
}

data class SpringConfigurationMetadata @JsonCreator constructor(
    @JsonProperty("properties")
    val properties: List<SpringConfigurationMetadataProperty> = mutableListOf(),
    @JsonProperty("hints")
    val hints: List<SpringConfigurationMetadataHint>?
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
    val values: List<ValueHint>?
)

class ValueHint @JsonCreator constructor(
    @JsonProperty("value")
    val value: String,
    @JsonProperty("description")
    val description: String?
)