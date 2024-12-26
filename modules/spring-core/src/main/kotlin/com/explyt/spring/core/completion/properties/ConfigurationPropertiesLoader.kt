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

import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.PrimitiveTypes
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil

interface ConfigurationPropertiesLoader {

    fun loadProperties(module: Module): List<ConfigurationProperty>

    fun loadPropertyHints(module: Module): List<PropertyHint>

    fun loadPropertyMetadataElements(module: Module): List<ElementHint>

    fun loadMetadataElements(module: Module): List<ElementHint>

    fun findMetadataValueElement(module: Module, propertyName: String, propertyValue: String): ElementHint?

    companion object {
        val EP_NAME = ProjectExtensionPointName<ConfigurationPropertiesLoader>(
            "com.explyt.spring.core.configurationPropertiesLoader"
        )

        fun getPropertyType(psiType: PsiType?): PropertyType? {
            if (psiType is PsiArrayType) return PropertyType.ARRAY
            return getPropertyType(psiType?.resolvedPsiClass)
        }

        fun getPropertyType(psiClass: PsiClass?): PropertyType? {
            return when {
                InheritanceUtil.isInheritor(psiClass, Map::class.java.name) -> PropertyType.MAP
                InheritanceUtil.isInheritor(psiClass, Iterable::class.java.name) -> PropertyType.LIST
                psiClass is PsiArrayType -> PropertyType.ARRAY
                else -> null
            }
        }
    }
}

data class ConfigurationProperty(
    val name: String,
    var propertyType: PropertyType?,
    var type: String?,
    var sourceType: String?,
    var description: String?,
    var defaultValue: Any?,
    var deprecation: DeprecationInfo?,
    val inLineYaml: Boolean = false
) {
    fun isMap() = propertyType == PropertyType.MAP

    fun isList() = propertyType == PropertyType.LIST

    fun isArray() = propertyType == PropertyType.ARRAY

    fun isBooleanType() = type == JavaCoreClasses.BOOLEAN || type == PrimitiveTypes.BOOLEAN
}

data class PropertyHint(
    val name: String,
    val values: List<ValueHint>,
    val providers: List<ProviderHint>
)

data class ElementHint(
    val name: String,
    val jsonProperty: JsonProperty
)

enum class PropertyType {
    LIST, MAP, ARRAY
}