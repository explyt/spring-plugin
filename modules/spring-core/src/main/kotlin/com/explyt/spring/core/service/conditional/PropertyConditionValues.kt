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
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

class PropertyConditionValues(private val module: Module) {
    private val propertiesByKey = DefinedConfigurationPropertiesSearch.getInstance(module.project)
        .getPropertiesCommonKeyMap(module)
    private val profilesService = ProfilesService.getInstance(module.project)
    private val yamlDocumentProfiles = HashMap<YAMLDocument, String?>()
    private val propertiesDocuments = HashMap<PropertiesFile, PropertiesDocuments>()

    fun valueOf(key: String): ConditionPropertyValue = when (val raw = rawValueOf(key)) {
        is ConditionPropertyValue.Known -> resolve(raw.text, 0)?.let { ConditionPropertyValue.Known(it) }
            ?: ConditionPropertyValue.Unresolvable

        else -> raw
    }

    private fun rawValueOf(key: String): ConditionPropertyValue {
        val defined = propertiesByKey[PropertyUtil.toCommonPropertyForm(key)].orEmpty()
        if (defined.any { isInUnclearFile(it) }) return ConditionPropertyValue.Unresolvable
        val loaded = defined.filter { isLoaded(it) }
        val winningFile = FoldedPropertyValue.choose(module, loaded)?.property?.psiElement?.containingFile
            ?: return ConditionPropertyValue.Missing
        val winner = loaded
            .filter { it.psiElement?.containingFile == winningFile }
            .maxByOrNull { it.psiElement?.textRange?.startOffset ?: -1 }
            ?: return ConditionPropertyValue.Missing
        return valueText(winner)?.let { ConditionPropertyValue.Known(it) } ?: ConditionPropertyValue.Unresolvable
    }

    private fun valueText(property: DefinedConfigurationProperty): String? {
        val element = property.psiElement ?: return null
        return if (element is Property) element.unescapedValue else property.value
    }

    private fun isLoaded(property: DefinedConfigurationProperty): Boolean {
        val fileProfile = FoldedPropertyValue.profileOf(property.sourceFile)
        if (fileProfile != null && !profilesService.compute(fileProfile)) return false
        val documentProfile = documentProfileOf(property.psiElement ?: return true) ?: return true
        return documentProfile.split(',').map { it.trim() }.filter { it.isNotEmpty() }.any { profilesService.compute(it) }
    }

    private fun isInUnclearFile(property: DefinedConfigurationProperty): Boolean {
        val file = property.psiElement?.containingFile as? PropertiesFile ?: return false
        return documentsOf(file).unclear
    }

    private fun documentProfileOf(element: PsiElement): String? {
        val yamlDocument = PsiTreeUtil.getParentOfType(element, YAMLDocument::class.java)
        if (yamlDocument != null) {
            return yamlDocumentProfiles.getOrPut(yamlDocument) {
                PsiTreeUtil.findChildrenOfType(yamlDocument, YAMLKeyValue::class.java)
                    .firstOrNull { it.value is YAMLScalar && YAMLUtil.getConfigFullName(it) in PROFILE_KEYS }
                    ?.valueText
            }
        }
        val file = element.containingFile as? PropertiesFile ?: return null
        return documentsOf(file).profileAt(element.textRange.startOffset)
    }

    private fun documentsOf(file: PropertiesFile): PropertiesDocuments =
        propertiesDocuments.getOrPut(file) { PropertiesDocuments.of(file) }

    private fun resolve(value: String, depth: Int): String? {
        if (depth > MAX_DEPTH || ESCAPED_PREFIX in value) return null
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
        if (ESCAPED_SEPARATOR in content) return null
        val separator = topLevelSeparator(content)
        val name = if (separator < 0) content else content.substring(0, separator)
        val default = if (separator < 0) null else content.substring(separator + 1)
        return when (val referenced = rawValueOf(name)) {
            is ConditionPropertyValue.Known -> resolve(referenced.text, depth + 1)
            ConditionPropertyValue.Unresolvable -> null
            ConditionPropertyValue.Missing -> default?.let { resolve(it, depth + 1) }
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

    private class PropertiesDocuments(
        private val boundaries: List<Int>,
        private val profiles: List<String?>,
        val unclear: Boolean
    ) {
        fun profileAt(offset: Int): String? = profiles[boundaries.count { it <= offset }]

        companion object {
            fun of(file: PropertiesFile): PropertiesDocuments {
                val lines = lineStarts(file.containingFile.text)
                val separators = lines.indices.filter { lines[it].second in DOCUMENT_SEPARATORS }
                val unclear = separators.any { index ->
                    listOf(index - 1, index + 1).any { neighbour ->
                        lines.getOrNull(neighbour)?.second?.let { isComment(it) } == true
                    }
                }
                val boundaries = separators.map { lines[it].first }
                val profiles = MutableList<String?>(boundaries.size + 1) { null }
                file.properties.forEach { property ->
                    if (property.key in PROFILE_KEYS) {
                        val document = boundaries.count { it <= property.psiElement.textRange.startOffset }
                        if (profiles[document] == null) {
                            profiles[document] = (property as? Property)?.unescapedValue ?: property.value
                        }
                    }
                }
                return PropertiesDocuments(boundaries, profiles, unclear)
            }

            private fun lineStarts(text: String): List<Pair<Int, String>> {
                val result = mutableListOf<Pair<Int, String>>()
                var start = 0
                while (start <= text.length) {
                    val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
                    result += start to text.substring(start, end).removeSuffix("\r")
                    start = end + 1
                }
                return result
            }

            private fun isComment(line: String): Boolean {
                val trimmed = line.trimStart()
                return trimmed.startsWith("#") || trimmed.startsWith("!")
            }
        }
    }

    private companion object {
        const val PREFIX = "\${"
        const val ESCAPED_PREFIX = "\\\${"
        const val ESCAPED_SEPARATOR = "\\:"
        const val MAX_DEPTH = 8
        val PROFILE_KEYS = setOf("spring.config.activate.on-profile", "spring.profiles")
        val DOCUMENT_SEPARATORS = setOf("#---", "!---")
    }
}
