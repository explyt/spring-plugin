/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

/**
 * The property naming strategies Jackson ships in `PropertyNamingStrategies`, translating a property name exactly as
 * Jackson's `NamingStrategyImpls` does, so a schema can name a property the way the wire does.
 *
 * Acronyms follow Jackson, not intuition: snake case keeps a leading run of capitals together (`URLValue` is
 * `urlvalue`), while kebab and dot case start a new word at the last capital of the run (`URLValue` is `url-value`).
 */
internal enum class JacksonNamingStrategy(private val strategyClassName: String) {
    LOWER_CAMEL_CASE("LowerCamelCaseStrategy") {
        override fun translate(name: String): String = name
    },
    UPPER_CAMEL_CASE("UpperCamelCaseStrategy") {
        override fun translate(name: String): String = name.replaceFirstChar { it.uppercaseChar() }
    },
    SNAKE_CASE("SnakeCaseStrategy") {
        override fun translate(name: String): String = snakeCase(name)
    },
    UPPER_SNAKE_CASE("UpperSnakeCaseStrategy") {
        override fun translate(name: String): String = snakeCase(name).uppercase()
    },
    LOWER_CASE("LowerCaseStrategy") {
        override fun translate(name: String): String = name.lowercase()
    },
    KEBAB_CASE("KebabCaseStrategy") {
        override fun translate(name: String): String = lowerCaseWithSeparator(name, '-')
    },
    LOWER_DOT_CASE("LowerDotCaseStrategy") {
        override fun translate(name: String): String = lowerCaseWithSeparator(name, '.')
    };

    abstract fun translate(name: String): String

    companion object {

        /**
         * The strategy an identifier names: a constant of `PropertyNamingStrategies` or of the deprecated
         * `PropertyNamingStrategy` holder, or a strategy class, simple or fully qualified with `.` or `$`.
         * `null` for anything else, such as a project's own strategy class, whose translation cannot be known.
         */
        fun named(identifier: String): JacksonNamingStrategy? {
            val simpleName = identifier.trim().substringAfterLast('.').substringAfterLast('$')
            return entries.firstOrNull { it.name == simpleName || it.strategyClassName == simpleName }
                ?: LEGACY_NAMES[simpleName]
        }

        private val LEGACY_NAMES = mapOf(
            "CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES" to SNAKE_CASE,
            "PASCAL_CASE_TO_CAMEL_CASE" to UPPER_CAMEL_CASE,
            "LowerCaseWithUnderscoresStrategy" to SNAKE_CASE,
            "PascalCaseStrategy" to UPPER_CAMEL_CASE,
        )

        private fun snakeCase(name: String): String {
            val result = StringBuilder(name.length * 2)
            var previousTranslated = false
            name.forEachIndexed { index, char ->
                if (index == 0 && char == '_') return@forEachIndexed
                if (char.isUpperCase()) {
                    if (!previousTranslated && result.isNotEmpty() && result.last() != '_') result.append('_')
                    result.append(char.lowercaseChar())
                    previousTranslated = true
                } else {
                    result.append(char)
                    previousTranslated = false
                }
            }
            return if (result.isNotEmpty()) result.toString() else name
        }

        private fun lowerCaseWithSeparator(name: String, separator: Char): String {
            val result = StringBuilder(name.length + (name.length shr 1))
            var capitalsInRow = 0
            name.forEachIndexed { index, char ->
                val lower = char.lowercaseChar()
                if (lower == char) {
                    if (capitalsInRow > 1) result.insert(result.length - 1, separator)
                    capitalsInRow = 0
                } else {
                    if (capitalsInRow == 0 && index > 0) result.append(separator)
                    capitalsInRow++
                }
                result.append(lower)
            }
            return result.toString()
        }
    }
}
