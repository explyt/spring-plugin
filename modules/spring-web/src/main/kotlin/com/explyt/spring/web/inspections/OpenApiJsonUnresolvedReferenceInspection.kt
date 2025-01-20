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

package com.explyt.spring.web.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.inspections.quickfix.AddJsonOpenApiElementQuickFix
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.JsonUtil
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType

class OpenApiJsonUnresolvedReferenceInspection : SpringBaseLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        val jsonFile = file as? JsonFile ?: return false

        return super.isAvailableForFile(file)
                && SpringWebUtil.isSpringWebProject(file.project)
                && OpenApiUtils.isOpenApi(jsonFile)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return JsonReferenceVisitor(holder)
    }

}

private class JsonReferenceVisitor(val problems: ProblemsHolder) : JsonElementVisitor() {

    override fun visitProperty(jsonProperty: JsonProperty) {
        if (jsonProperty.name != "\$ref") return
        val valueText = jsonProperty.value?.let { ExplytPsiUtil.getUnquotedText(it) } ?: return
        if (!valueText.startsWith("#/")) return
        val jsonFile = jsonProperty.containingFile as? JsonFile ?: return

        val refText = valueText.substring(2)
        val refPath = refText.split('/')
        val rootObject = JsonUtil.getTopLevelObject(jsonFile) ?: return
        val parentProperty = rootObject.findProperty(refPath[0])
        if (!refExists(parentProperty, refPath)) {
            problems.registerProblem(
                jsonProperty.navigationElement,
                SpringWebBundle.message("explyt.spring.web.inspection.openapi.json.reference.error", refText),
                ProblemHighlightType.ERROR,
                *getQuickFixes(rootObject, refPath)
            )
        }
    }

    private fun refExists(parentProperty: JsonProperty?, refPath: List<String>): Boolean {
        if (parentProperty == null) return false

        var subProperty = parentProperty
        for (i in 1..<refPath.size) {
            subProperty = getSubProperty(subProperty, refPath[i])
        }
        return subProperty != null
    }

    private fun getSubProperty(jsonProperty: JsonProperty?, key: String): JsonProperty? {
        return jsonProperty?.value
            ?.childrenOfType<JsonProperty>()
            ?.firstOrNull { it.name == key }
    }

    private fun getQuickFixes(rootObject: JsonObject, refPath: List<String>): Array<LocalQuickFix> {
        if (refPath.size != 3) return emptyArray()
        if (refPath[0] != "components") return emptyArray()
        if (refPath[1] !in SUPPORTED_COMPONENTS) return emptyArray()
        if (refPath[2].isBlank()) return emptyArray()

        return arrayOf(AddJsonOpenApiElementQuickFix(rootObject, refPath))
    }

    companion object {
        private val SUPPORTED_COMPONENTS = setOf(
            "schemas", "responses", "parameters", "examples", "requestBodies"
        )
    }

}
