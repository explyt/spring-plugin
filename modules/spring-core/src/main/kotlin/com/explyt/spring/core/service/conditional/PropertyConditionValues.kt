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
        val winner = FoldedPropertyValue.choose(module, loaded)?.property ?: return null
        val winnerFile = winner.psiElement?.containingFile
        val profileOverride = loaded.lastOrNull {
            it.psiElement?.containingFile == winnerFile && documentProfileOf(it) != null
        }
        return (profileOverride ?: winner).value
    }

    private fun isLoaded(property: DefinedConfigurationProperty): Boolean {
        val fileProfile = FoldedPropertyValue.profileOf(property.sourceFile)
        if (fileProfile != null && !profilesService.compute(fileProfile)) return false
        val documentProfile = documentProfileOf(property) ?: return true
        return documentProfile.split(',').map { it.trim() }.filter { it.isNotEmpty() }.any { profilesService.compute(it) }
    }

    private fun documentProfileOf(property: DefinedConfigurationProperty): String? {
        val document = PsiTreeUtil.getParentOfType(property.psiElement, YAMLDocument::class.java) ?: return null
        return documentProfiles.getOrPut(document) {
            PsiTreeUtil.findChildrenOfType(document, YAMLKeyValue::class.java)
                .firstOrNull { it.value is YAMLScalar && YAMLUtil.getConfigFullName(it) in PROFILE_KEYS }
                ?.valueText
        }
    }

    private fun resolve(value: String, depth: Int): String? {
        if (depth > MAX_DEPTH) return null
        val result = StringBuilder()
        var index = 0
        while (index < value.length) {
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
        const val MAX_DEPTH = 8
        val PROFILE_KEYS = setOf("spring.config.activate.on-profile", "spring.profiles")
    }
}
