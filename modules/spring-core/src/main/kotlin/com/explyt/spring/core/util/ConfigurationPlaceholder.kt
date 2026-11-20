/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.SpringProperties.SPEL_PREFIX
import com.explyt.spring.core.properties.providers.ValueConfigurationPropertyReferenceProvider
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.impl.YAMLHashImpl

data class ConfigurationPlaceholder(
    val name: String,
    val defaultValue: String?,
    val range: TextRange,
    val redundantRanges: List<TextRange>,
) {
    fun replacementDefault(element: PsiElement, valueRange: TextRange): String? {
        val value = defaultValue ?: return null
        if (PLACEHOLDER_PREFIX in value || SPEL_PREFIX in value) return null
        return when (element.containingFile) {
            is PropertiesFile -> value.takeUnless {
                '\\' in it || it.startsWith('=') || it.startsWith(':') || it.firstOrNull()?.isWhitespace() == true
            }
            is YAMLFile -> value.takeIf { isSafeYamlDefault(element, valueRange, it) }
            else -> null
        }
    }

    private fun isSafeYamlDefault(element: PsiElement, valueRange: TextRange, value: String): Boolean {
        if (value.isNotEmpty() && !YAML_PLAIN_TEXT.matches(value)) return false
        if (value in YAML_LONE_INDICATORS || value.lowercase() in YAML_IMPLICIT_WORDS) return false
        val unsigned = value.removePrefix("-")
        if (unsigned.removePrefix(".").firstOrNull()?.isDigit() == true) {
            val isSimpleDecimal = YAML_DECIMAL_INTEGER.matches(value) ||
                YAML_DECIMAL_FRACTION.matches(value) && value.toDoubleOrNull()?.toString() == value
            if (!isSimpleDecimal && !YAML_DOTTED_STRING.matches(value)) return false
        }
        val entry = element.parent as? YAMLKeyValue ?: return false
        val editedScalar = valueRange.replace(element.text, value)
        val entryRange = element.textRange.shiftLeft(entry.textRange.startOffset)
        val editedEntry = entryRange.replace(entry.text, editedScalar)
        val yaml = if (entry.parent is YAMLHashImpl) "{$editedEntry}" else "$editedEntry\n"
        val parsed = YAMLElementGenerator.getInstance(element.project).createDummyYamlWithText(yaml)
        if (PsiTreeUtil.hasErrorElements(parsed)) return false
        val mapping = parsed.documents.singleOrNull()?.topLevelValue as? YAMLMapping ?: return false
        val parsedEntry = mapping.keyValues.singleOrNull() ?: return false
        if (parsedEntry.key?.text != entry.key?.text) return false
        val parsedValue = parsedEntry.value
        return if (editedScalar.isEmpty()) parsedValue == null
        else parsedValue is YAMLScalar && parsedValue.text == editedScalar
    }

    companion object {
        private val YAML_PLAIN_TEXT = Regex("[A-Za-z0-9._/:-]+")
        private val YAML_LONE_INDICATORS = setOf("-", "?", ":")
        private val YAML_IMPLICIT_WORDS = setOf("y", "yes", "n", "no", "on", "off", "true", "false", "null", ".inf", "-.inf", ".nan")
        private val YAML_DECIMAL_INTEGER = Regex("0|-?[1-9][0-9]*")
        private val YAML_DECIMAL_FRACTION = Regex("""-?(?:0|[1-9][0-9]*)\.[0-9]+""")
        private val YAML_DOTTED_STRING = Regex("""[0-9]+(?:\.[0-9]+){2,}""")

        fun parse(value: String): ConfigurationPlaceholder? {
            val matcher = ValueConfigurationPropertyReferenceProvider.PROPERTIES_PATTERN.matcher(value)
            if (!matcher.matches()) return null
            val default = matcher.group(2)?.removePrefix(":")
            val range = TextRange(0, value.length)
            val redundantRanges = if (default == null) listOf(range) else listOf(
                TextRange(0, matcher.start(2) + 1),
                TextRange(value.lastIndex, value.length),
            )
            return ConfigurationPlaceholder(matcher.group(1), default, range, redundantRanges)
        }
    }
}
