package com.esprito.spring.core.completion.properties

import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module

interface ConfigurationFactoriesNamesLoader {

    fun loadFactories(module: Module): Set<String>

    companion object {
        val EP_NAME = ProjectExtensionPointName<ConfigurationFactoriesNamesLoader>(
            "com.esprito.spring.core.configurationFactoriesNamesLoader"
        )
    }
}