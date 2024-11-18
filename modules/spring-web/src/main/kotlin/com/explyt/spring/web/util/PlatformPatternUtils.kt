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

package com.explyt.spring.web.util

import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonPsiUtil
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

object PlatformPatternUtils {

    fun openApiYamlInnerRef() =
        psiElement(YAMLScalar::class.java)
            .with(isYamlInnerRef())
            .inFile(openApiYamlFile())
            .withParent(yamlKeyNamedRef())

    fun isYamlInnerRef(): PatternCondition<in YAMLScalar> =
        object : PatternCondition<YAMLScalar>("isYamlInnerRef") {
            override fun accepts(yamlScalar: YAMLScalar, context: ProcessingContext?): Boolean {
                return yamlScalar.textValue.startsWith("#/")
            }

        }

    fun openApiYamlFile(): ElementPattern<out PsiFile> {
        return PlatformPatterns.psiFile(YAMLFile::class.java)
            .with(object : PatternCondition<YAMLFile>("isOpenApiYamlFile") {
                override fun accepts(yamlFile: YAMLFile, context: ProcessingContext?): Boolean {
                    return OpenApiUtils.isOpenApi(yamlFile)
                }
            })
    }

    fun yamlKeyNamedRef(): ElementPattern<out YAMLKeyValue> {
        return psiElement(YAMLKeyValue::class.java)
            .with(object : PatternCondition<YAMLKeyValue>("isYamlKeyNamed") {
                override fun accepts(yamlKeyValue: YAMLKeyValue, context: ProcessingContext?): Boolean {
                    return yamlKeyValue.keyText == "\$ref"
                }
            })
    }

    fun openApiJsonInnerRef() = psiElement(JsonStringLiteral::class.java)
        .with(isJsonInnerRef())
        .inFile(openApiJsonFile())
        .withParent(jsonKeyNamedRef())

    fun isJsonInnerRef(): PatternCondition<in JsonStringLiteral> =
        object : PatternCondition<JsonStringLiteral>("isJsonInnerRef") {
            override fun accepts(literal: JsonStringLiteral, context: ProcessingContext?): Boolean {
                return JsonPsiUtil.isPropertyValue(literal) && literal.value.startsWith("#/")
            }

        }

    fun openApiJsonFile(): ElementPattern<out PsiFile> {
        return PlatformPatterns.psiFile(JsonFile::class.java)
            .with(object : PatternCondition<JsonFile>("isOpenApiJsonFile") {
                override fun accepts(jsonFile: JsonFile, context: ProcessingContext?): Boolean {
                    return OpenApiUtils.isOpenApi(jsonFile)
                }
            })
    }

    fun jsonKeyNamedRef(): ElementPattern<out JsonProperty> {
        return psiElement(JsonProperty::class.java)
            .with(object : PatternCondition<JsonProperty>("isJsonKeyNamed") {
                override fun accepts(jsonProperty: JsonProperty, context: ProcessingContext?): Boolean {
                    return jsonProperty.name == "\$ref"
                }
            })
    }

}