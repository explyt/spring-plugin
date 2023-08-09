package com.esprito.spring.core.completion.properties

import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module

interface ConfigurationPropertiesLoader {

    fun loadProperties(module: Module): List<ConfigurationProperty>

    fun loadPropertyHints(module: Module): List<PropertyHint>

    companion object {
        val EP_NAME = ProjectExtensionPointName<ConfigurationPropertiesLoader>("com.esprito.spring.core.configurationPropertiesLoader")
    }
}

data class ConfigurationProperty(val name: String,
                                 var type: String?,
                                 var sourceType: String?,
                                 var description: String?,
                                 var defaultValue: Any?)

data class PropertyHint(val name: String,
                        val values: List<ValueHint>,
                        val providers: List<ProviderHint>)