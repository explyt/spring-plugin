/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.utils

import com.intellij.psi.PsiClass

/**
 * Shared naming and lookup helpers for the quick fixes of [com.explyt.spring.core.inspections.SpringBeanSystemEnvironmentAccessInspection].
 */
object SystemEnvironmentAccessFixUtil {

    const val ENVIRONMENT_CLASS = "org.springframework.core.env.Environment"
    const val AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired"
    const val GET_PROPERTY = "getProperty"

    private const val FALLBACK_MEMBER_NAME = "property"

    private val SEPARATOR = Regex("[^A-Za-z0-9]+")

    /** Keys that cannot be used as a field name without escaping. */
    private val RESERVED_NAMES = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "fun", "goto", "if",
        "implements", "import", "in", "instanceof", "int", "interface", "is", "long", "native", "new", "object",
        "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
        "synchronized", "this", "throw", "throws", "transient", "try", "typeof", "val", "var", "void", "volatile",
        "when", "while",
    )

    /** `SOME_VAR` -> `someVar`, `some.var` -> `someVar`: the member that will hold the value. */
    fun deriveMemberName(key: String): String {
        val segments = key.split(SEPARATOR).filter { it.isNotEmpty() }
        if (segments.isEmpty()) return FALLBACK_MEMBER_NAME

        val builder = StringBuilder(segments.first().lowercase())
        for (segment in segments.drop(1)) {
            builder.append(segment.lowercase().replaceFirstChar { it.uppercaseChar() })
        }

        var name = builder.toString()
        if (name.first().isDigit()) name = "p$name"
        if (name in RESERVED_NAMES) name = "${name}Value"
        return name
    }

    fun uniqueMemberName(existing: Set<String>, base: String): String {
        if (base !in existing) return base
        var index = 2
        while ("$base$index" in existing) index++
        return "$base$index"
    }

    /**
     * The name of the `Environment` the class already depends on, as a field or as a constructor parameter, or `null`
     * when the class has none. Read from the light class so that both Java and Kotlin are covered.
     */
    fun environmentMemberName(psiClass: PsiClass): String? {
        psiClass.fields.firstOrNull { it.type.canonicalText == ENVIRONMENT_CLASS }?.name?.let { return it }
        return psiClass.constructors.asSequence()
            .flatMap { it.parameterList.parameters.asSequence() }
            .firstOrNull { it.type.canonicalText == ENVIRONMENT_CLASS }
            ?.name
    }
}
