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

package com.explyt.spring.web.action

import com.explyt.spring.core.action.JavaMethodGenerateUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses.PATH_VARIABLE
import com.explyt.spring.web.SpringWebClasses.REQUEST_MAPPING
import com.explyt.spring.web.SpringWebClasses.REQUEST_PARAM
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import io.ktor.http.*
import io.ktor.util.*


class SpringWebJavaMethodGenerateAction : BaseGenerateAction(WebMethodHandler()) {
    init {
        getTemplatePresentation().text = SpringWebBundle.message("explyt.spring.action.web.method.generate")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return file.isWritable
                && super.isValidForFile(project, editor, file)
                && SpringWebUtil.isSpringWebProject(project)
    }

    override fun isValidForClass(targetClass: PsiClass?) = targetClass?.language == JavaLanguage.INSTANCE

    companion object {
        val urlValidator = object : InputValidator {
            override fun checkInput(inputString: String) = !inputString.contains("\\")
            override fun canClose(inputString: String) = true
        }

        fun parseUrl(urlString: String): HttMethod {
            val url = Url(urlString)
            val methodName = url.pathSegments.last().replace("{", "").replace("}", "").replace("-", "").replace("_", "")
            val value = getMappingHttpValue(urlString)
            return HttMethod(methodName, value, getHttpParams(url))
        }

        private fun getHttpParams(url: Url): List<HttpParam> {
            return url.pathSegments
                .filter { it.startsWith("{") && it.endsWith("}") }
                .map { HttpParam(false, it.replace("{", "").replace("}", ""), null) } +
                    url.parameters.toMap().map { HttpParam(true, it.key, it.value.joinToString(",")) }
        }

        private fun getMappingHttpValue(urlString: String): String {
            return if (urlString.contains("?")) urlString.substringBefore("?") else urlString
        }
    }
}

data class HttMethod(val name: String, val mappingValue: String, val params: List<HttpParam>)

data class HttpParam(val query: Boolean, val name: String, val value: String?)

private class WebMethodHandler : CodeInsightActionHandler {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GENERATE_WEB_METHOD)
        ApplicationManager.getApplication().invokeLater {
            val tittle = SpringWebBundle.message("explyt.spring.action.web.method.generate.dialog.tittle")
            val urlMessage = SpringWebBundle.message("explyt.spring.action.web.method.generate.dialog.url")
            val urlValidator = SpringWebJavaMethodGenerateAction.urlValidator
            val urlString = Messages.showInputDialog(urlMessage, tittle, null, null, urlValidator)
                ?.takeIf { it.isNotEmpty() } ?: return@invokeLater
            val httMethod = SpringWebJavaMethodGenerateAction.parseUrl(urlString)

            runWriteAction {
                val targetClass = JavaMethodGenerateUtils.getNearTargetClass(editor, file) ?: return@runWriteAction
                val documentManager = PsiDocumentManager.getInstance(project)
                val document = documentManager.getDocument(file) ?: return@runWriteAction
                PsiDocumentManager.getInstance(file.project).commitDocument(document)

                val offsetToInsert = JavaMethodGenerateUtils.findOffsetToInsertMethod(editor, file, targetClass)
                val template = getTemplate(project, httMethod, targetClass.isInterface) ?: return@runWriteAction

                editor.caretModel.moveToOffset(offsetToInsert)

                JavaMethodGenerateUtils.startTemplate(project, editor, template)
            }
        }
    }

    private fun getTemplate(project: Project, httMethod: HttMethod, isInterface: Boolean): Template? {
        val template = TemplateManager.getInstance(project).createTemplate("", "")

        template.addTextSegment("@$REQUEST_MAPPING(\n")
        template.addTextSegment("value = \"${httMethod.mappingValue}\",\n")
        template.addTextSegment("method = org.springframework.web.bind.annotation.RequestMethod.GET\n")
        template.addTextSegment(")\n")
        if (isInterface) template.addTextSegment("String ") else template.addTextSegment("public String ")
        val nameExpr = ConstantNode(httMethod.name)
        template.addVariable("name", nameExpr, nameExpr, true)
        template.addTextSegment("(")
        if (httMethod.params.isNotEmpty()) {
            template.addTextSegment("\n")
            for ((index, param) in httMethod.params.withIndex()) {
                if (param.query) {
                    template.addTextSegment("@$REQUEST_PARAM(name = \"${param.name}\", defaultValue = \"${param.value ?: ""}\") String ${param.name}")
                } else {
                    template.addTextSegment("@$PATH_VARIABLE(\"${param.name}\") String ${param.name}")
                }
                if (index != httMethod.params.size - 1) {
                    template.addTextSegment(",\n")
                }
            }
            template.addTextSegment("\n")
        }
        template.addTextSegment(")")
        if (isInterface) {
            template.addTextSegment(";")
        } else {
            template.addTextSegment("{ return null; }")
        }

        template.setToIndent(true)
        template.isToReformat = true
        template.isToShortenLongNames = true
        return template
    }
}