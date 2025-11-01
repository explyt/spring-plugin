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

    fun getAllPropertiesSystemEnvironment(module: Module): List<String> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                getAllProperties(module).map { PropertyUtil.toSystemEnvironmentForm(it.name) },
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun findProperty(module: Module, propertyName: String): ConfigurationProperty? {
        return getAllProperties(module).find { isSameProperty(it.name, propertyName, it.type) }
    }

    fun findHint(module: Module, propertyName: String): PropertyHint? {
        return getAllHints(module).find { isSameProperty(it.name, propertyName) }
    }

    fun getAllHints(module: Module): List<PropertyHint> {
        return ConfigurationPropertiesLoader.EP_NAME.getExtensions(module.project)
            .flatMap { it.loadPropertyHints(module) }
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