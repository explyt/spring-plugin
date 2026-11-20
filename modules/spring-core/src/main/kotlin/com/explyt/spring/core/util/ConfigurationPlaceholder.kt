/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.SpringProperties.SPEL_PREFIX
import com.explyt.spring.core.properties.providers.ValueConfigurationPropertyReferenceProvider
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLParserDefinition
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLFile

data class ConfigurationPlaceholder(
    val name: String,
    val defaultValue: String?,
    val range: TextRange,
    val redundantRanges: List<TextRange>,
) {
    fun replacementDefault(file: PsiFile): String? {
        val value = defaultValue ?: return null
        if (PLACEHOLDER_PREFIX in value || SPEL_PREFIX in value) return null
        return when (file) {
            is PropertiesFile -> value.takeUnless { '\\' in it || it.firstOrNull()?.isWhitespace() == true }
            is YAMLFile -> value.takeIf { it.isEmpty() || isSafeYamlDefault(file.project, it) }
            else -> null
        }
    }

    private fun isSafeYamlDefault(project: Project, value: String): Boolean {
        if (!YAML_PLAIN_TEXT.matches(value) || value.lowercase() in YAML_IMPLICIT_WORDS) return false
        val unsigned = value.removePrefix("-")
        if (unsigned.removePrefix(".").firstOrNull()?.isDigit() == true) {
            val isSimpleDecimal = YAML_DECIMAL_INTEGER.matches(value) ||
                YAML_DECIMAL_FRACTION.matches(value) && value.toDoubleOrNull()?.toString() == value
            if (!isSimpleDecimal && !YAML_DOTTED_STRING.matches(value)) return false
        }
        val prefix = "value: "
        val lexer = YAMLParserDefinition().createLexer(project)
        lexer.start(prefix + value)
        while (lexer.tokenType != null && lexer.tokenStart < prefix.length) lexer.advance()
        return lexer.tokenType == YAMLTokenTypes.TEXT && lexer.tokenStart == prefix.length &&
            lexer.tokenEnd == prefix.length + value.length
    }

    companion object {
        private val YAML_PLAIN_TEXT = Regex("[A-Za-z0-9._/:-]+")
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
