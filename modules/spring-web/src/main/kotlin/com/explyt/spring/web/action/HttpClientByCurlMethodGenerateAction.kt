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

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.action.JavaCoreMethodGenerateUtils
import com.explyt.spring.core.action.KotlinMethodGenerateUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses.JAVA_HTTP_CLIENT
import com.explyt.spring.web.parser.CurlParser
import com.explyt.spring.web.parser.HttpMethod
import com.explyt.spring.web.parser.HttpParamType
import com.explyt.spring.web.parser.UrlParser
import com.explyt.util.JavaMethodGenerateUtils
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile

class HttpClientByCurlJavaMethodGenerateAction : BaseGenerateAction(HttpClientMethodHandler()) {
    init {
        getTemplatePresentation().text = SpringWebBundle.message("explyt.http.client.action.web.method.generate")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return file.isWritable
                && super.isValidForFile(project, editor, file)
                && LibraryClassCache.searchForLibraryClass(project, JAVA_HTTP_CLIENT) != null
    }

    override fun isValidForClass(targetClass: PsiClass?) = targetClass?.language == JavaLanguage.INSTANCE
}

class HttpClientByCurlKotlinMethodGenerateAction : BaseGenerateAction(HttpClientMethodHandler()) {
    init {
        getTemplatePresentation().text = SpringWebBundle.message("explyt.http.client.action.web.method.generate")
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (!file.isWritable || file !is KtFile || file.isCompiled) return false
        return LibraryClassCache.searchForLibraryClass(project, JAVA_HTTP_CLIENT) != null
    }
}

private class HttpClientMethodHandler : CodeInsightActionHandler {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GENERATE_HTTP_CLIENT_METHOD)
        ApplicationManager.getApplication().invokeLater {
            val tittle = SpringWebBundle.message("explyt.http.client.action.web.method.generate")
            val urlMessage = SpringWebBundle.message("explyt.spring.action.web.method.generate.dialog.url")
            val urlValidator = SpringWebJavaMethodGenerateAction.urlValidator
            val urlString = Messages.showMultilineInputDialog(project, urlMessage, tittle, null, null, urlValidator)
                ?.takeIf { it.isNotEmpty() } ?: return@invokeLater
            val httpMethod = if (urlString.startsWith("curl"))
                CurlParser.parse(urlString) else UrlParser.parse(urlString)

            if (file.language == JavaLanguage.INSTANCE) {
                runWriteAction {
                    val targetClass =
                        JavaCoreMethodGenerateUtils.getNearTargetClass(editor, file) ?: return@runWriteAction
                    val documentManager = PsiDocumentManager.getInstance(project)
                    val document = documentManager.getDocument(file) ?: return@runWriteAction
                    PsiDocumentManager.getInstance(file.project).commitDocument(document)

                    val offsetToInsert = JavaMethodGenerateUtils.findOffsetToInsertMethod(editor, file, targetClass)
                    val template = getTemplate(project, httpMethod, false) ?: return@runWriteAction

                    editor.caretModel.moveToOffset(offsetToInsert)

                    JavaMethodGenerateUtils.startTemplate(project, editor, template)
                }
            }
            if (file.language == KotlinLanguage.INSTANCE) {
                runWriteAction {
                    val targetClass = KotlinMethodGenerateUtils.getTargetClass(editor, file)
                    val documentManager = PsiDocumentManager.getInstance(project)
                    val document = documentManager.getDocument(file) ?: return@runWriteAction
                    PsiDocumentManager.getInstance(file.project).commitDocument(document)

                    val offsetToInsert = KotlinMethodGenerateUtils.findOffsetToInsertMethod(editor, file, targetClass)
                    val template = getTemplate(project, httpMethod, true) ?: return@runWriteAction

                    editor.caretModel.moveToOffset(offsetToInsert)

                    KotlinMethodGenerateUtils.startTemplate(project, editor, template)
                }
            }
        }
    }

    private fun getTemplate(project: Project, httpMethod: HttpMethod, isKotlin: Boolean): Template? {
        val headers = httpMethod.params.filter { it.type == HttpParamType.HEADER }
        val variable = if (isKotlin) "val" else "var"
        val endOfExpression = if (isKotlin) "\n" else ";\n"
        val template = TemplateManager.getInstance(project).createTemplate("", "")
        if (isKotlin) {
            template.addTextSegment("fun main() {\n")
        } else {
            template.addTextSegment("public static void main(String[] args) throws Exception {\n")
        }

        template.addTextSegment("$variable request = java.net.http.HttpRequest.newBuilder()\n")
        template.addTextSegment(".uri(java.net.URI.create(\"${httpMethod.url}\"))\n")
        template.addTextSegment(".method(\"${httpMethod.type}\", ${getMethodBodyPublisher(httpMethod)})\n")
        for (header in headers) {
            template.addTextSegment(".setHeader(\"${header.name}\", \"${header.value ?: ""}\")\n")
        }
        if (httpMethod.timeOutSec > 0) {
            template.addTextSegment(".timeout(java.time.Duration.ofSeconds(${httpMethod.timeOutSec}))\n")
        }
        template.addTextSegment(".build()$endOfExpression")
        template.addTextSegment(variable + " " + getHttpClient(httpMethod) + endOfExpression)
        template.addTextSegment(
            "$variable response = client.send(request, " + getBodyHandler(httpMethod) + " )$endOfExpression"
        )
        if (isKotlin) {
            template.addTextSegment("println(response.body())$endOfExpression")
        } else {
            template.addTextSegment("System.out.println(response.body())$endOfExpression")
        }
        template.addTextSegment("}\n")


        template.setToIndent(true)
        template.isToReformat = true
        template.isToShortenLongNames = true
        template.toString()
        return template
    }

    private fun getBodyHandler(httpMethod: HttpMethod): String {
        return if (httpMethod.outPut.isEmpty()) {
            "java.net.http.HttpResponse.BodyHandlers.ofString()"
        } else {
            "java.net.http.HttpResponse.BodyHandlers.ofFile(java.nio.file.Path.of(\"${httpMethod.outPut}\"))"
        }
    }

    private fun getHttpClient(httpMethod: HttpMethod): String {
        return if (httpMethod.redirect) {
            "client = java.net.http.HttpClient.newBuilder()\n" +
                    "    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)\n" +
                    "    .build()"
        } else {
            "client = java.net.http.HttpClient.newHttpClient()"
        }
    }

    private fun getMethodBodyPublisher(httpMethod: HttpMethod): String {
        val dataValue = httpMethod.params.find { it.type == HttpParamType.DATA }?.value?.replace("\"", "\\\"")
        return if (dataValue.isNullOrEmpty()) {
            "java.net.http.HttpRequest.BodyPublishers.noBody()"
        } else {
            "java.net.http.HttpRequest.BodyPublishers.ofString(\"$dataValue\")"
        }
    }
}