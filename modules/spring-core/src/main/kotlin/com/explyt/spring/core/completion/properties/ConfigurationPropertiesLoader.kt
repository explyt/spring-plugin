package com.explyt.spring.core.completion.properties

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

    companion object {
        val EP_NAME = ProjectExtensionPointName<ConfigurationPropertiesLoader>(
            "com.explyt.spring.core.configurationPropertiesLoader"
        )

        fun getPropertyType(psiType: PsiType?): PropertyType? {
            if (psiType is PsiArrayType) return PropertyType.ARRAY
            return getPropertyType(psiType?.resolvedPsiClass)
        }

        fun getPropertyType(psiClass: PsiClass?): PropertyType? {
            return if (InheritanceUtil.isInheritor(psiClass, Map::class.java.name)) {
                PropertyType.MAP
            } else if (InheritanceUtil.isInheritor(psiClass, Iterable::class.java.name)) {
                PropertyType.LIST
            } else if (psiClass is PsiArrayType) {
                PropertyType.ARRAY
            } else {
                null
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