package com.esprito.spring.core.completion.properties

import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module

interface ConfigurationPropertiesLoader {

    fun loadProperties(module: Module): List<ConfigurationProperty>

    companion object {
        val EP_NAME = ProjectExtensionPointName<ConfigurationPropertiesLoader>("com.esprito.spring.core.configurationPropertiesLoader")
    }
}

data class ConfigurationProperty(val name: String,
                                 val type: String?,
                                 val description: String?,
                                 val defaultValue: Any?) {

}