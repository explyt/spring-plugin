package com.esprito.spring.web.util

import com.esprito.spring.web.editor.openapi.OpenApiUtils
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