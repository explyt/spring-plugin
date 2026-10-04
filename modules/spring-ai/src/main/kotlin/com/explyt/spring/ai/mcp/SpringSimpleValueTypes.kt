/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.InheritanceUtil

/**
 * Spring's notion of a "simple" type, which decides how an unannotated handler parameter is bound.
 *
 * A port of `BeanUtils.isSimpleProperty` and `ClassUtils.isSimpleValueType` of Spring Framework 6.2 and 7.0, whose
 * lists are identical: a primitive or its wrapper, `Enum`, `CharSequence`, `Number`, `Date`, `Temporal`, `ZoneId`,
 * `TimeZone`, `File`, `Path`, `Charset`, `Currency` and `InetAddress` with their subtypes, exactly `URI`, `URL`, `UUID`,
 * `Locale`, `Pattern` and `Class`, and an array of any of these. `void` and `Void` are never simple.
 */
internal object SpringSimpleValueTypes {

    private val ASSIGNABLE_TO = listOf(
        "java.lang.Enum",
        "java.lang.CharSequence",
        "java.lang.Number",
        "java.util.Date",
        "java.time.temporal.Temporal",
        "java.time.ZoneId",
        "java.util.TimeZone",
        "java.io.File",
        "java.nio.file.Path",
        "java.nio.charset.Charset",
        "java.util.Currency",
        "java.net.InetAddress",
    )

    private val EXACTLY = setOf(
        "java.net.URI",
        "java.net.URL",
        "java.util.UUID",
        "java.util.Locale",
        "java.util.regex.Pattern",
        CommonClassNames.JAVA_LANG_CLASS,
    )

    private val PRIMITIVE_WRAPPERS = setOf(
        CommonClassNames.JAVA_LANG_BOOLEAN,
        CommonClassNames.JAVA_LANG_BYTE,
        CommonClassNames.JAVA_LANG_CHARACTER,
        CommonClassNames.JAVA_LANG_SHORT,
        CommonClassNames.JAVA_LANG_INTEGER,
        CommonClassNames.JAVA_LANG_LONG,
        CommonClassNames.JAVA_LANG_FLOAT,
        CommonClassNames.JAVA_LANG_DOUBLE,
    )

    /** `BeanUtils.isSimpleProperty`: a simple value type, or an array whose component is one. */
    fun isSimpleProperty(type: PsiType): Boolean =
        isSimpleValueType(type) || (type is PsiArrayType && isSimpleValueType(type.componentType))

    /** `ClassUtils.isSimpleValueType`, for a type the IDE can read; an unresolved class is not simple. */
    fun isSimpleValueType(type: PsiType): Boolean = when (type) {
        is PsiPrimitiveType -> type != PsiTypes.voidType()
        is PsiClassType -> {
            val qualifiedName = type.resolve()?.qualifiedName
            qualifiedName != null && (
                    qualifiedName in PRIMITIVE_WRAPPERS ||
                            qualifiedName in EXACTLY ||
                            ASSIGNABLE_TO.any { InheritanceUtil.isInheritor(type, it) })
        }

        else -> false
    }
}
