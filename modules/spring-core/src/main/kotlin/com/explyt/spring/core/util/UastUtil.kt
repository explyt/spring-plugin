/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.util

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
        if (value.startsWith("#{")) return null
        if (value.startsWith("\${") && value.endsWith("}")) {
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