/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.SpringProperties.POSTFIX_KEYS
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.PropertyUtil.isSameProperty
import com.intellij.lang.properties.IProperty
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

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
            .flatMap { it.loadProperties(module) }
    }

    fun getAllPropertiesSystemEnvironment(module: Module): Collection<String> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                getEnvProperties(module),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getEnvProperties(module: Module): Collection<String> {
        val propertiesByModule = getAllProperties(module).map { PropertyUtil.toSystemEnvironmentForm(it.name) }
        val propertiesByApplicationFiles = DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getAllDefinedConfigurationProperty()
            .map { PropertyUtil.toSystemEnvironmentForm(it.key) }
        val configurationPropertiesByProject = (ConfigurationPropertiesLoader.EP_NAME
            .findExtension(ProjectConfigurationPropertiesLoader::class.java, module.project)
            ?.loadAllPropertiesFromProject(module.project) ?: emptyList())
            .map { PropertyUtil.toSystemEnvironmentForm(it.name) }
        return (propertiesByModule + propertiesByApplicationFiles + configurationPropertiesByProject).toSet()
    }

    fun findProperty(module: Module, propertyName: String): ConfigurationProperty? {
        return getPropertyIndex(module).findProperty(propertyName)
    }

    /**
     * The catalogue runs to thousands of entries, and the scan this replaces re-normalised every one of them on
     * every lookup — `isSameProperty` lowercases and strips separators on both sides per candidate. The names on
     * the catalogue side never change between lookups, so they are normalised once per module instead.
     */
    private fun getPropertyIndex(module: Module): ConfigurationPropertyIndex {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                ConfigurationPropertyIndex.of(getAllProperties(module)),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun findHint(module: Module, propertyName: String): PropertyHint? {
        return getAllHints(module).find { isSameProperty(it.name, propertyName) }
    }

    fun getAllHints(module: Module): List<PropertyHint> {
        return ConfigurationPropertiesLoader.EP_NAME.getExtensions(module.project)
            .flatMap { it.loadPropertyHints(module) }
    }

    /**
     * Hint lookups by exact name, for callers that would otherwise walk [getAllHints] once per configuration key.
     * Most keys have no hint, so those walks always ran to the end of the list.
     */
    fun getHintIndex(module: Module): PropertyHintIndex {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                PropertyHintIndex.of(getAllHints(module)),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getElementNameHints(module: Module): List<ElementHint> {
        return ConfigurationPropertiesLoader.EP_NAME.getExtensions(module.project)
            .flatMap { it.loadMetadataElements(module) }
    }

    fun getElementNameProperties(module: Module): List<ElementHint> {
        return ConfigurationPropertiesLoader.EP_NAME.getExtensions(module.project)
            .flatMap { it.loadPropertyMetadataElements(module) }
    }

    fun findElementHintValue(module: Module, propertyName: String, value: String): ElementHint? {
        return ConfigurationPropertiesLoader.EP_NAME.getExtensions(module.project)
            .firstNotNullOfOrNull { it.findMetadataValueElement(module, propertyName, value) }
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
            ConfigurationProperty("$basePropertyName.${it.value}", null, null, null, it.description, null, null)
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