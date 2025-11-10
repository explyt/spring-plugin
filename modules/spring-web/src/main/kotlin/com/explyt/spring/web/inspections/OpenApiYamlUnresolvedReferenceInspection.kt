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
import com.explyt.plugin.PluginIds
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.inspections.quickfix.AddYamlOpenApiElementQuickFix
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class OpenApiYamlUnresolvedReferenceInspection : SpringBaseLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        val yamlFile = file as? YAMLFile ?: return false

        return super.isAvailableForFile(file)
                && PluginIds.SPRING_WEB_JB.isNotEnabled()
                && SpringWebUtil.isSpringWebProject(file.project)
                && OpenApiUtils.isOpenApi(yamlFile)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return YamlReferenceVisitor(holder)
    }

}

private class YamlReferenceVisitor(val problems: ProblemsHolder) : YamlPsiElementVisitor() {

    override fun visitKeyValue(keyValue: YAMLKeyValue) {
        if (keyValue.keyText != "\$ref") return
        val valueText = keyValue.valueText
        if (!valueText.startsWith("#/")) return
        val yamlFile = keyValue.containingFile as? YAMLFile ?: return

        val refText = valueText.substring(2)
        val refPath = refText.split('/')

        if (YAMLUtil.getQualifiedKeyInFile(yamlFile, refPath) == null) {
            problems.registerProblem(
                keyValue,
                SpringWebBundle.message("explyt.spring.web.inspection.openapi.yaml.reference.error", refText),
                ProblemHighlightType.ERROR,
                *getQuickFixes(yamlFile, refPath)
            )
        }
    }

    private fun getQuickFixes(yamlFile: YAMLFile, refPath: List<String>): Array<LocalQuickFix> {
        if (refPath.size != 3) return emptyArray()
        if (refPath[0] != "components") return emptyArray()
        if (refPath[1] !in SUPPORTED_COMPONENTS) return emptyArray()
        if (refPath[2].isBlank()) return emptyArray()

        return arrayOf(AddYamlOpenApiElementQuickFix(yamlFile, refPath))
    }

    companion object {
        private val SUPPORTED_COMPONENTS = setOf(
            "schemas", "responses", "parameters", "examples", "requestBodies"
        )
    }

}
