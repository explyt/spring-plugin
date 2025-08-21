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

import com.explyt.spring.core.action.JavaCoreMethodGenerateUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses.PATH_VARIABLE
import com.explyt.spring.web.SpringWebClasses.REQUEST_BODY
import com.explyt.spring.web.SpringWebClasses.REQUEST_HEADER
import com.explyt.spring.web.SpringWebClasses.REQUEST_MAPPING
import com.explyt.spring.web.SpringWebClasses.REQUEST_PARAM
import com.explyt.spring.web.parser.CurlParser
import com.explyt.spring.web.parser.HttpMethod
import com.explyt.spring.web.parser.HttpParamType
import com.explyt.spring.web.parser.UrlParser
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.JavaMethodGenerateUtils
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


class SpringWebJavaMethodGenerateAction : BaseGenerateAction(WebMethodHandler()) {
    init {
        getTemplatePresentation().text = SpringWebBundle.message("explyt.spring.action.web.method.generate")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return file.isWritable
                && super.isValidForFile(project, editor, file)
                && SpringWebUtil.isWebRequestModule(file)
    }

    override fun isValidForClass(targetClass: PsiClass?) = targetClass?.language == JavaLanguage.INSTANCE

    companion object {
        val urlValidator = object : InputValidator {
            override fun checkInput(inputString: String): Boolean {
                if (inputString.startsWith("curl")) {
                    try {
                        CurlParser.parse(inputString)
                        return true
                    } catch (_: Exception) {
                        return false
                    }
                }
                return !inputString.contains("\\")
            }

            override fun canClose(inputString: String) = true
        }
    }
}

private class WebMethodHandler : CodeInsightActionHandler {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GENERATE_WEB_METHOD)
        ApplicationManager.getApplication().invokeLater {
            val tittle = SpringWebBundle.message("explyt.spring.action.web.method.generate.dialog.tittle")
            val urlMessage = SpringWebBundle.message("explyt.spring.action.web.method.generate.dialog.url")
            val urlValidator = SpringWebJavaMethodGenerateAction.urlValidator
            val urlString = Messages.showMultilineInputDialog(project, urlMessage, tittle, null, null, urlValidator)
                ?.takeIf { it.isNotEmpty() } ?: return@invokeLater
            val httpMethod = if (urlString.startsWith("curl"))
                CurlParser.parse(urlString) else UrlParser.parse(urlString)

            runWriteAction {
                val targetClass = JavaCoreMethodGenerateUtils.getNearTargetClass(editor, file) ?: return@runWriteAction
                val documentManager = PsiDocumentManager.getInstance(project)
                val document = documentManager.getDocument(file) ?: return@runWriteAction
                PsiDocumentManager.getInstance(file.project).commitDocument(document)

                val offsetToInsert = JavaMethodGenerateUtils.findOffsetToInsertMethod(editor, file, targetClass)
                val template = getTemplate(project, httpMethod, targetClass.isInterface) ?: return@runWriteAction

                editor.caretModel.moveToOffset(offsetToInsert)

                JavaMethodGenerateUtils.startTemplate(project, editor, template)
            }
        }
    }

    private fun getTemplate(project: Project, httpMethod: HttpMethod, isInterface: Boolean): Template? {
        val template = TemplateManager.getInstance(project).createTemplate("", "")

        template.addTextSegment("@$REQUEST_MAPPING(\n")
        template.addTextSegment("value = \"${httpMethod.getMappingHttpValue()}\",\n")
        template.addTextSegment("method = org.springframework.web.bind.annotation.RequestMethod.${httpMethod.type}\n")
        template.addTextSegment(")\n")
        if (isInterface) template.addTextSegment("String ") else template.addTextSegment("public String ")
        val nameExpr = ConstantNode(httpMethod.name)
        template.addVariable("name", nameExpr, nameExpr, true)
        template.addTextSegment("(")
        if (httpMethod.params.isNotEmpty()) {
            template.addTextSegment("\n")
            for ((index, param) in httpMethod.params.withIndex()) {
                when (param.type) {
                    HttpParamType.QUERY -> {
                        template.addTextSegment(
                            "@$REQUEST_PARAM(name = \"${param.name}\", required = false," +
                                    "defaultValue = \"${param.value ?: ""}\") String ${param.toJavaIdentifier()}"
                        )
                    }

                    HttpParamType.PATH -> {
                        template.addTextSegment("@$PATH_VARIABLE(\"${param.name}\") String ${param.toJavaIdentifier()}")
                    }

                    HttpParamType.HEADER -> {
                        template.addTextSegment(
                            "@$REQUEST_HEADER(name = \"${param.name}\", required = false," +
                                    "defaultValue = \"${param.value ?: ""}\") String ${param.toJavaIdentifier()}"
                        )
                    }

                    HttpParamType.DATA -> template.addTextSegment("@$REQUEST_BODY(required = false) String requestBody")
                }
                if (index != httpMethod.params.size - 1) {
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