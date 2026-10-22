/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.SpringProperties.PLACEHOLDER_PREFIX
import com.explyt.spring.core.SpringProperties.PLACEHOLDER_SUFFIX
import com.explyt.spring.core.SpringProperties.SPEL_PREFIX
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UCallExpression
import com.intellij.patterns.uast.UExpressionPattern
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.evaluateString

object UastUtil {
    fun UExpression.getPropertyValue(): String? {
        val value = this.evaluateString() ?: return null
        // SpEL is evaluated at runtime against the bean graph, so the value is not statically known and every
        // caller here validates a literal. Returning null stops the validation rather than reporting a guess.
        if (value.startsWith(SPEL_PREFIX)) return null
        if (value.startsWith(PLACEHOLDER_PREFIX) && value.endsWith(PLACEHOLDER_SUFFIX)) {
            val matchResult = PropertyUtil.VALUE_REGEX.matchEntire(value) ?: return null
            val (key, defaultValue) = matchResult.destructured

            val psiElement = this.sourcePsi ?: return null
            val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null
            val propertyInfo = DefinedConfigurationPropertiesSearch.getInstance(module.project)
                .findProperties(module, key)
                .firstOrNull()
            return (propertyInfo?.value ?: defaultValue).takeIf { it.isNotEmpty() }
        }
        return value
    }

    fun UCallExpression.getArgumentValueAsEnumName(index: Int): String? =
        getArgumentForParameter(index)
            ?.asSourceString()
            ?.split('.')
            ?.last()


    class UPolyadicExpressionPattern : UExpressionPattern<UPolyadicExpression, UPolyadicExpressionPattern>(
        UPolyadicExpression::class.java
    )

}