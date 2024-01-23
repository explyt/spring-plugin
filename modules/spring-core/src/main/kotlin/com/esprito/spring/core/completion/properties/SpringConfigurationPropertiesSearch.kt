package com.esprito.spring.core.completion.properties

import com.esprito.spring.core.SpringProperties.POSTFIX_KEYS
import com.intellij.json.psi.JsonProperty
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

    /**
     * return all configuration properties with their map sub-key.
     * example: logging.level, logging.level.sql.
     */
    fun getAllPropertiesWithSubKeys(module: Module): List<ConfigurationProperty> {
        return getAllProperties(module) + getKeysProperty(module)
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
            .flatMap { it.loadPropertyHints(module) }
    }

    fun getElementNameHints(module: Module): List<JsonProperty> {
        return ConfigurationPropertiesLoader.EP_NAME.getExtensions(module.project)
            .flatMap { it.loadMetadataElements(module) }
    }

    private fun getKeysProperty(module: Module): List<ConfigurationProperty> {
        return getAllHints(module).asSequence()
            .filter { it.name.endsWith(POSTFIX_KEYS) }
            .flatMap { toConfigurationPropertyList(it) }
            .toList()
    }

    private fun toConfigurationPropertyList(hint: PropertyHint): List<ConfigurationProperty> {
        val basePropertyName = hint.name.substringBefore(POSTFIX_KEYS)
        return hint.values.map {
            ConfigurationProperty("$basePropertyName.${it.value}", null, null, it.description, null, null)
        }
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