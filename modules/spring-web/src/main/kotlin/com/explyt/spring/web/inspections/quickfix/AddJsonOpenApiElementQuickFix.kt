/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.quickfix

import com.explyt.spring.core.inspections.utils.ExplytJsonUtil.addPropertyToObject
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticUtil.registerActionUsage
import com.explyt.spring.web.SpringWebBundle
import com.explyt.util.ExplytPsiUtil
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class AddJsonOpenApiElementQuickFix(rootObject: JsonObject, private val refPath: List<String>) :
    LocalQuickFixAndIntentionActionOnPsiElement(rootObject) {

    override fun getFamilyName(): String =
        SpringWebBundle.message("explyt.spring.web.inspection.openapi.json.reference.fix.family")

    override fun getText(): String =
        SpringWebBundle.message("explyt.spring.web.inspection.openapi.json.reference.fix.text", refPath.last())

    override fun invoke(
        project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement
    ) {
        val rootObject = startElement as? JsonObject ?: return

        editor.registerActionUsage(
            StatisticActionId.QUICK_FIX_OPENAPI_GENERATE_COMPONENT, null
        )

        val (_, componentType, componentName) = refPath

        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            when (componentType) {
                "schemas" -> addSchema(rootObject, componentName)
                "responses" -> addResponse(rootObject, componentName)
                "parameters" -> addParameter(rootObject, componentName)
                "examples" -> addExample(rootObject, componentName)
                "requestBodies" -> addRequestBody(rootObject, componentName)
            }
        }
    }

    private fun addSchema(rootObject: JsonObject, componentName: String) {
        val componentType = "schemas"
        val components = rootObject.findProperty("components")?.value as? JsonObject
        if (components == null) {
            addAndNavigate(
                rootObject, "components", """
                {
                  "$componentType": {
                    "$componentName": {
                      "type": "object"   
                    }
                  }
                }
                """.trimIndent()
            )
        } else {
            val schemasObject = components.findProperty(componentType)?.value as? JsonObject
            if (schemasObject == null) {
                addAndNavigate(
                    components, componentType, """
                    {
                      "$componentName": {
                        "type": "object" 
                      }
                    }
                    """.trimIndent()
                )
            } else {
                addAndNavigate(
                    schemasObject, componentName, """
                        {
                          "type": "object"
                        }
                        """.trimIndent()
                )
            }
        }
    }

    private fun addResponse(rootObject: JsonObject, componentName: String) {
        val componentType = "responses"
        val componentsObject = rootObject.findProperty("components")?.value as? JsonObject
        if (componentsObject == null) {
            addAndNavigate(
                rootObject, "components", """
                {
                  "$componentType": {
                    "$componentName": {
                      "description": "$componentName"
                    }
                  }
                }
                """.trimIndent()
            )
        } else {
            val responsesObject = componentsObject.findProperty(componentType)?.value as? JsonObject
            if (responsesObject == null) {
                addAndNavigate(
                    componentsObject, componentType, """
                    {
                      "$componentName": {
                        "description": "$componentName"
                      }
                    }
                    """.trimIndent()
                )
            } else {
                addAndNavigate(
                    responsesObject, componentName, """
                        {
                          "description": "$componentName"
                        }
                        """.trimIndent()
                )
            }
        }
    }

    private fun addParameter(rootObject: JsonObject, componentName: String) {
        val componentType = "parameters"
        val componentsObject = rootObject.findProperty("components")?.value as? JsonObject
        if (componentsObject == null) {
            addAndNavigate(
                rootObject, "components", """
                {
                  "$componentType": {
                    "$componentName": {
                      "name": "${componentName.lowercase()}",
                      "in": "query",
                      "required": false,
                      "schema": {
                        "type": "object"
                      }
                    }
                  }
                }
                """.trimIndent()
            )
        } else {
            val parametersObject = componentsObject.findProperty(componentType)?.value as? JsonObject
            if (parametersObject == null) {
                addAndNavigate(
                    componentsObject, componentType, """
                    {
                      "$componentName": {
                        "name": "${componentName.lowercase()}",
                        "in": "query",
                        "required": false,
                        "schema": {
                          "type": "object"
                        }
                      }
                    }
                    """.trimIndent()
                )
            } else {
                addAndNavigate(
                    parametersObject, componentName, """
                    {
                      "name": "${componentName.lowercase()}",
                      "in": "query",
                      "required": false,
                      "schema": {
                        "type": "object"
                      }
                    }
                    """.trimIndent()
                )
            }
        }
    }

    private fun addExample(rootObject: JsonObject, componentName: String) {
        val componentType = "examples"
        val componentsObject = rootObject.findProperty("components")?.value as? JsonObject
        if (componentsObject == null) {
            addAndNavigate(
                rootObject, "components", """
                {
                  "$componentType": {
                    "$componentName": {}
                  }  
                }
                """.trimIndent()
            )
        } else {
            val examplesObject = componentsObject.findProperty(componentType)?.value as? JsonObject
            if (examplesObject == null) {
                addAndNavigate(
                    componentsObject, componentType, """
                    {
                      "$componentName": {}
                    }  
                    """.trimIndent()
                )
            } else {
                addAndNavigate(examplesObject, componentName, "{}")
            }
        }
    }

    private fun addRequestBody(rootObject: JsonObject, componentName: String) {
        val componentType = "requestBodies"
        val componentsObject = rootObject.findProperty("components")?.value as? JsonObject
        if (componentsObject == null) {
            addAndNavigate(
                rootObject, "components", """
                {
                  "$componentType": {
                    "$componentName": {
                      "content": {}
                    }
                  }
                }
                """.trimIndent()
            )
        } else {
            val requestBodiesObject = componentsObject.findProperty(componentType)?.value as? JsonObject
            if (requestBodiesObject == null) {
                addAndNavigate(
                    componentsObject, componentType, """
                    {
                      "$componentName": {
                        "content": {}
                      }
                    }
                    """.trimIndent()
                )
            } else {
                addAndNavigate(
                    requestBodiesObject, componentName, """
                    {
                      "content": {}
                    }
                    """.trimIndent()
                )
            }
        }
    }

    private fun addAndNavigate(
        parentObject: JsonObject, key: String, value: String
    ) {
        val generator = JsonElementGenerator(parentObject.project)
        val newProperty = generator.createProperty(key, value)

        addPropertyToObject(newProperty, parentObject, generator)?.let {
            ExplytPsiUtil.navigate(it)
        }
    }

}