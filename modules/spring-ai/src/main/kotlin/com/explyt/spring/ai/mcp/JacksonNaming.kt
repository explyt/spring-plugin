/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.JacksonClasses
import com.explyt.spring.core.properties.FoldedPropertyValue
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression

/**
 * A Jackson property naming strategy as the project declares it.
 *
 * [strategy] is `null` for a strategy that cannot be read statically - a project's own strategy class, an unknown
 * value - which is reported by name as `UNKNOWN` and leaves the names as declared, rather than guessing.
 *
 * @property source where the strategy was declared: `@JsonNaming` or `spring.jackson.property-naming-strategy`.
 */
internal class JacksonNaming private constructor(
    val name: String,
    val source: String,
    private val strategy: JacksonNamingStrategy?,
) {
    /** The name Jackson writes [declaredName] under, or `null` when the strategy cannot be read. */
    fun translate(declaredName: String): String? = strategy?.translate(declaredName)

    companion object {
        private const val ANNOTATION_SOURCE = "@JsonNaming"
        private const val PROPERTY = "spring.jackson.property-naming-strategy"
        private const val UNKNOWN = "UNKNOWN"

        /** The strategy `@JsonNaming` declares on [psiClass] or a superclass, or `null` when none does. */
        fun declaredOn(psiClass: PsiClass): JacksonNaming? {
            val annotation = AnnotationUtil.findAnnotationInHierarchy(psiClass, JacksonClasses.JSON_NAMING_ANNOTATIONS)
                ?: return null
            val value = annotation.findAttributeValue("value")
            val strategyClass = (value as? PsiClassObjectAccessExpression)?.operand?.type?.canonicalText
                ?: value?.text?.removeSuffix("::class")?.removeSuffix(".class")
            return of(strategyClass, ANNOTATION_SOURCE)
        }

        /** The strategy the module's configuration declares, or `null` when it declares none. */
        fun configuredFor(module: Module): JacksonNaming? {
            val value = FoldedPropertyValue.resolve(module, PROPERTY)?.value?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            return of(value, PROPERTY)
        }

        private fun of(declared: String?, source: String): JacksonNaming {
            val strategy = declared?.let(JacksonNamingStrategy::named)
            return JacksonNaming(strategy?.name ?: UNKNOWN, source, strategy)
        }
    }
}
