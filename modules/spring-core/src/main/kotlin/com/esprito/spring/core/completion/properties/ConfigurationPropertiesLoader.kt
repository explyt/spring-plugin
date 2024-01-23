package com.esprito.spring.core.completion.properties

import com.esprito.spring.core.JavaCoreClasses
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module

interface ConfigurationPropertiesLoader {

    fun loadProperties(module: Module): List<ConfigurationProperty>

    fun loadPropertyHints(module: Module): List<PropertyHint>

    fun loadMetadataElements(module: Module): List<JsonProperty>

    companion object {
        val EP_NAME = ProjectExtensionPointName<ConfigurationPropertiesLoader>(
            "com.esprito.spring.core.configurationPropertiesLoader"
        )
    }
}

data class ConfigurationProperty(
    val name: String,
    var type: String?,
    var sourceType: String?,
    var description: String?,
    var defaultValue: Any?,
    var deprecation: DeprecationInfo?,
    val inLineYaml: Boolean = false
) {
    fun isMap(): Boolean {
        return type?.startsWith(JavaCoreClasses.MAP) == true
    }

    fun isList(): Boolean {
        return type?.startsWith(JavaCoreClasses.LIST) == true
    }

    fun isArray(): Boolean {
        return type?.endsWith("[]") == true
    }
}

data class PropertyHint(
    val name: String,
    val values: List<ValueHint>,
    val providers: List<ProviderHint>
)