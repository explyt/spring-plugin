/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.properties.FoldedPropertyValue
import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

class PropertyConditionValues(private val module: Module) {
    private val propertiesByKey = DefinedConfigurationPropertiesSearch.getInstance(module.project)
        .getPropertiesCommonKeyMap(module)
    private val profilesService = ProfilesService.getInstance(module.project)
    private val documentProfiles = HashMap<YAMLDocument, String?>()

    fun valueOf(key: String): ConditionPropertyValue {
        val raw = rawValueOf(key) ?: return ConditionPropertyValue.Missing
        return resolve(raw, 0)?.let { ConditionPropertyValue.Known(it) } ?: ConditionPropertyValue.Unresolvable
    }

    private fun rawValueOf(key: String): String? {
        val loaded = propertiesByKey[PropertyUtil.toCommonPropertyForm(key)].orEmpty().filter { isLoaded(it) }
        val winner = loaded.minWithOrNull(compareBy({ sourceProfilePriority(it) }, { documentProfilePriority(it) }, { it.sourceFile }))
        return winner?.let { rawValue(it) }
    }

    private fun rawValue(property: DefinedConfigurationProperty): String? {
        val element = property.psiElement ?: return property.value
        if (element.containingFile is PropertiesFile) {
            return element.text.substringAfter('=', property.value.orEmpty())
        }
        return property.value
    }

    private fun sourceProfilePriority(property: DefinedConfigurationProperty): Int =
        FoldedPropertyValue.profileOf(property.sourceFile)?.let { if (profilesService.compute(it)) 0 else 2 } ?: 1

    private fun documentProfilePriority(property: DefinedConfigurationProperty): Int =
        documentProfileOf(property)?.let { if (isProfileActive(it)) 0 else 2 } ?: 1

    private fun isLoaded(property: DefinedConfigurationProperty): Boolean {
        val fileProfile = FoldedPropertyValue.profileOf(property.sourceFile)
        if (fileProfile != null && !profilesService.compute(fileProfile)) return false
        val documentProfile = documentProfileOf(property) ?: return true
        return isProfileActive(documentProfile)
    }

    private fun isProfileActive(expression: String): Boolean =
        expression.split(',').map { it.trim() }.filter { it.isNotEmpty() }.any { profilesService.compute(it) }

    private fun documentProfileOf(property: DefinedConfigurationProperty): String? {
        val element = property.psiElement ?: return null
        val yamlDocument = PsiTreeUtil.getParentOfType(element, YAMLDocument::class.java)
        if (yamlDocument != null) {
            return documentProfiles.getOrPut(yamlDocument) {
                PsiTreeUtil.findChildrenOfType(yamlDocument, YAMLKeyValue::class.java)
                    .firstOrNull { it.value is YAMLScalar && YAMLUtil.getConfigFullName(it) in PROFILE_KEYS }
                    ?.valueText
            }
        }
        val propertiesFile = element.containingFile as? PropertiesFile ?: return null
        val text = propertiesFile.text
        val separator = DOCUMENT_SEPARATORS
            .mapNotNull { marker -> text.indexOf("\n$marker").takeIf { it >= 0 }?.plus(1) }
            .filter { it < element.textRange.startOffset }
            .maxOrNull()
            ?: return null
        val nextSeparator = DOCUMENT_SEPARATORS
            .mapNotNull { marker -> text.indexOf("\n$marker", separator + 1).takeIf { it >= 0 }?.plus(1) }
            .minOrNull() ?: text.length
        return propertiesFile.properties
            .filter { it.psiElement.textRange.startOffset in separator until nextSeparator }
            .filter { it.key in PROFILE_KEYS }
            .mapNotNull { it.value }
            .firstOrNull()
    }

    private fun resolve(value: String, depth: Int): String? {
        if (depth > MAX_DEPTH) return null
        val result = StringBuilder()
        var index = 0
        while (index < value.length) {
            if (value[index] == ESCAPE && value.startsWith(PREFIX, index + 1)) {
                result.append(PREFIX)
                index += PREFIX.length + 1
                continue
            }
            val start = value.indexOf(PREFIX, index)
            if (start < 0) {
                result.append(value, index, value.length)
                break
            }
            val end = closingBrace(value, start + PREFIX.length) ?: return null
            result.append(value, index, start)
            result.append(resolvePlaceholder(value.substring(start + PREFIX.length, end), depth) ?: return null)
            index = end + 1
        }
        return result.toString()
    }

    private fun resolvePlaceholder(content: String, depth: Int): String? {
        val separator = topLevelSeparator(content)
        val name = if (separator < 0) content else content.substring(0, separator)
        val default = if (separator < 0) null else content.substring(separator + 1)
        val referenced = rawValueOf(name)
        return when {
            referenced != null -> resolve(referenced, depth + 1)
            default != null -> resolve(default, depth + 1)
            else -> null
        }
    }

    private fun closingBrace(value: String, from: Int): Int? {
        var nesting = 0
        var index = from
        while (index < value.length) {
            if (value[index] == ESCAPE && value.startsWith(PREFIX, index + 1)) {
                index += PREFIX.length + 1
                continue
            }
            when {
                value.startsWith(PREFIX, index) -> {
                    nesting++
                    index += PREFIX.length
                    continue
                }
                value[index] == '}' -> if (nesting == 0) return index else nesting--
            }
            index++
        }
        return null
    }

    private fun topLevelSeparator(content: String): Int {
        var nesting = 0
        var index = 0
        while (index < content.length) {
            when {
                content.startsWith(PREFIX, index) -> {
                    nesting++
                    index += PREFIX.length
                    continue
                }
                content[index] == '}' -> nesting--
                content[index] == ':' && nesting == 0 -> return index
            }
            index++
        }
        return -1
    }

    private companion object {
        const val PREFIX = "\${"
        const val ESCAPE = '\\'
        const val MAX_DEPTH = 8
        val PROFILE_KEYS = setOf("spring.config.activate.on-profile", "spring.profiles")
        val DOCUMENT_SEPARATORS = setOf("#---", "!---")
    }
}
