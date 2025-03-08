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

package com.explyt.spring.web.providers

import com.explyt.spring.web.httpclient.action.HttpRunFileAction
import com.explyt.spring.web.language.http.psi.HttpRequest
import com.explyt.spring.web.language.http.psi.HttpRequestLine
import com.explyt.spring.web.language.http.psi.HttpTypes
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.base.psi.getLineNumber

class HttpRunLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(psiElement: PsiElement): Info? {
        val leafElement = psiElement as? LeafPsiElement ?: return null
        if (leafElement.tokenType == HttpTypes.REQUEST_TARGET) return null
        val httpRequestLineElement = leafElement.parentOfType<HttpRequestLine>() ?: return null
        val httpRequest = httpRequestLineElement.parent as? HttpRequest ?: return null
        val file = leafElement.containingFile.virtualFile ?: return null

        val line = httpRequest.getLineNumber(true)

        return Info(
            object : AnAction({ "Run request" }, AllIcons.RunConfigurations.TestState.Run) {
                override fun actionPerformed(e: AnActionEvent) {
                    HttpRunFileAction.runHttpCommand(httpRequest.project, file, line)
                }
            }
        )
    }

}