package com.esprito.spring.core.completion.properties

import com.intellij.lang.properties.IProperty
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class SpringConfigurationPropertiesSearch {

    companion object {
        fun getInstance(project: Project): SpringConfigurationPropertiesSearch = project.service()
    }

    fun getAllProperties(module: Module): List<ConfigurationProperty> {
        return ConfigurationPropertiesLoader.EP_NAME.getExtensions(module.project)
            .asSequence().flatMap {
                it.loadProperties(module)
            }.toList()
    }

    fun findProperty(module: Module, propertyName: String): ConfigurationProperty? {
        return getAllProperties(module).find { it.name == propertyName }
    }

    fun findHint(module: Module, propertyName: String): PropertyHint? {
        return getAllHints(module).find { it.name == propertyName }
    }

    fun getAllHints(module: Module): List<PropertyHint> {
        return ConfigurationPropertiesLoader.EP_NAME.getExtensions(module.project)
            .asSequence().flatMap {
                it.loadPropertyHints(module)
            }.toList()
    }

    fun getAllFactoriesNames(module: Module): Set<String> {
        return ConfigurationFactoriesNamesLoader.EP_NAME.getExtensions(module.project)
            .flatMapTo(HashSet()) { it.loadFactories(module) }
    }

    fun getAllFactoriesMetadataFiles(module: Module): List<IProperty> {
        return ConfigurationFactoriesNamesLoader.EP_NAME.getExtensions(module.project)
            .flatMap { it.findMetadataProperties(module) }
    }

}