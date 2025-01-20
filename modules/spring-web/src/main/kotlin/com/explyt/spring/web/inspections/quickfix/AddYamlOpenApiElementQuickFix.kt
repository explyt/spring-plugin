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

package com.explyt.spring.web.inspections.quickfix

import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticUtil.registerActionUsage
import com.explyt.spring.web.SpringWebBundle
import com.explyt.util.ExplytPsiUtil
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

class AddYamlOpenApiElementQuickFix(yamlFile: YAMLFile, private val refPath: List<String>) :
    LocalQuickFixAndIntentionActionOnPsiElement(yamlFile) {

    override fun getFamilyName(): String =
        SpringWebBundle.message("explyt.spring.web.inspection.openapi.yaml.reference.fix.family")

    override fun getText(): String =
        SpringWebBundle.message("explyt.spring.web.inspection.openapi.yaml.reference.fix.text", refPath.last())

    override fun invoke(
        project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement
    ) {
        val yamlFile = file as? YAMLFile ?: return
        editor.registerActionUsage(
            StatisticActionId.QUICK_FIX_OPENAPI_GENERATE_COMPONENT, null
        )

        val (_, componentType, componentName) = refPath

        val rootMapping = yamlFile.documents.firstOrNull()?.topLevelValue as? YAMLMapping ?: return

        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            when (componentType) {
                "schemas" -> addSchema(rootMapping, componentName)
                "responses" -> addResponse(rootMapping, componentName)
                "parameters" -> addParameter(rootMapping, componentName)
                "examples" -> addExample(rootMapping, componentName)
                "requestBodies" -> addRequestBody(rootMapping, componentName)
            }
        }
    }

    private fun addSchema(rootMapping: YAMLMapping, componentName: String) {
        val componentType = "schemas"
        val componentsMapping = rootMapping.getKeyValueByKey("components")?.value as? YAMLMapping
        if (componentsMapping == null) {
            addAndNavigate(
                rootMapping, "components", """
                $componentType:
                  $componentName:
                    type: object    
                """.trimIndent()
            )
        } else {
            val schemasMapping = componentsMapping.getKeyValueByKey(componentType)?.value as? YAMLMapping
            if (schemasMapping == null) {
                addAndNavigate(
                    componentsMapping, componentType, """
                    $componentName:
                      type: object    
                    """.trimIndent()
                )
            } else {
                addAndNavigate(
                    schemasMapping, componentName, "type: object"
                )
            }
        }
    }

    private fun addResponse(rootMapping: YAMLMapping, componentName: String) {
        val componentType = "responses"
        val componentsMapping = rootMapping.getKeyValueByKey("components")?.value as? YAMLMapping
        if (componentsMapping == null) {
            addAndNavigate(
                rootMapping, "components", """
                $componentType:
                  $componentName:
                    description: $componentName
                """.trimIndent()
            )
        } else {
            val responsesMapping = componentsMapping.getKeyValueByKey(componentType)?.value as? YAMLMapping
            if (responsesMapping == null) {
                addAndNavigate(
                    componentsMapping, componentType, """
                    $componentName:
                      description: $componentName
                    """.trimIndent()
                )
            } else {
                addAndNavigate(
                    responsesMapping, componentName, "description: $componentName"
                )
            }
        }
    }

    private fun addParameter(rootMapping: YAMLMapping, componentName: String) {
        val componentType = "parameters"
        val componentsMapping = rootMapping.getKeyValueByKey("components")?.value as? YAMLMapping
        if (componentsMapping == null) {
            addAndNavigate(
                rootMapping, "components", """
                $componentType:
                  $componentName:
                    name: ${componentName.lowercase()}
                    in: query
                    required: false
                    schema:
                      type: object
                """.trimIndent()
            )
        } else {
            val parametersMapping = componentsMapping.getKeyValueByKey(componentType)?.value as? YAMLMapping
            if (parametersMapping == null) {
                addAndNavigate(
                    componentsMapping, componentType, """
                    $componentName:
                      name: ${componentName.lowercase()}
                      in: query
                      required: false
                      schema:
                        type: object
                    """.trimIndent()
                )
            } else {
                addAndNavigate(
                    parametersMapping, componentName, """
                    name: ${componentName.lowercase()}
                    in: query
                    required: false
                    schema:
                      type: object
                    """.trimIndent()
                )
            }
        }
    }

    private fun addExample(rootMapping: YAMLMapping, componentName: String) {
        val componentType = "examples"
        val componentsMapping = rootMapping.getKeyValueByKey("components")?.value as? YAMLMapping
        if (componentsMapping == null) {
            addAndNavigate(
                rootMapping, "components", """
                $componentType:
                  $componentName:
                """.trimIndent()
            )
        } else {
            val examplesMapping = componentsMapping.getKeyValueByKey(componentType)?.value as? YAMLMapping
            if (examplesMapping == null) {
                addAndNavigate(
                    componentsMapping, componentType, """
                    $componentName:
                    """.trimIndent()
                )
            } else {
                addAndNavigate(examplesMapping, componentName, "")
            }
        }
    }

    private fun addRequestBody(rootMapping: YAMLMapping, componentName: String) {
        val componentType = "requestBodies"
        val componentsMapping = rootMapping.getKeyValueByKey("components")?.value as? YAMLMapping
        if (componentsMapping == null) {
            addAndNavigate(
                rootMapping, "components", """
                $componentType:
                  $componentName:
                    content:
                """.trimIndent()
            )
        } else {
            val requestBodiesMapping = componentsMapping.getKeyValueByKey(componentType)?.value as? YAMLMapping
            if (requestBodiesMapping == null) {
                addAndNavigate(
                    componentsMapping, componentType, """
                    $componentName:
                      content:
                    """.trimIndent()
                )
            } else {
                addAndNavigate(
                    requestBodiesMapping, componentName, "content:\n"
                )
            }
        }
    }

    private fun addAndNavigate(
        rootMapping: YAMLMapping, key: String, value: String
    ) {
        val keyValueGenerator = YAMLElementGenerator.getInstance(rootMapping.project)

        val yamlKeyValue = keyValueGenerator.createYamlKeyValue(key, value)

        rootMapping.putKeyValue(yamlKeyValue)
        ExplytPsiUtil.navigate(rootMapping.children.lastOrNull())
    }

}