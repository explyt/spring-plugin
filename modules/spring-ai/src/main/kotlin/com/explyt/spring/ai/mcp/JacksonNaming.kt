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
 * value, a mapper the project configures in code - which is reported by name as `UNKNOWN` and leaves the names as
 * declared, rather than guessing.
 *
 * @property source where the strategy was declared: `@JsonNaming`, `spring.jackson.property-naming-strategy`, or
 * the bean that configures the mapper and may replace or override the property.
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

        /**
         * The strategy the module's configuration declares, or `null` when it declares none. A mapper, builder or
         * builder customizer bean of the project outranks the property, since it may replace or override it, and
         * makes the strategy unknown; the property stays named so it is not taken for absent.
         */
        fun configuredFor(module: Module): JacksonNaming? {
            val property = FoldedPropertyValue.resolve(module, PROPERTY)?.value?.trim()?.takeIf { it.isNotEmpty() }
            val customization = JacksonMapperCustomizations.declaredIn(module).firstOrNull()
            return when {
                customization != null -> JacksonNaming(UNKNOWN, customizedSource(customization, property), null)
                property != null -> of(property, PROPERTY)
                else -> null
            }
        }

        private fun customizedSource(customization: String, property: String?): String =
            customization + (property?.let { ", which may replace or override $PROPERTY=$it" } ?: "")

        private fun of(declared: String?, source: String): JacksonNaming {
            val strategy = declared?.let(JacksonNamingStrategy::named)
            return JacksonNaming(strategy?.name ?: UNKNOWN, source, strategy)
        }
    }
}
