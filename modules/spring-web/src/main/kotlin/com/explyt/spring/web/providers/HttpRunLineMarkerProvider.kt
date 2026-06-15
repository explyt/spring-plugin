/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.web.httpclient.action.HttpRunFileAction
import com.explyt.spring.web.language.http.psi.HttpRequest
import com.explyt.spring.web.language.http.psi.HttpRequestLine
import com.explyt.spring.web.language.http.psi.HttpRequestTarget
import com.explyt.spring.web.language.http.psi.HttpTypes
import com.explyt.spring.web.language.http.psi.HttpVariable
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

        val convertedElement = when (leafElement.tokenType) {
            HttpTypes.REQUEST_TARGET_VALUE -> psiElement
            HttpTypes.IDENTIFIER           -> leafElement.parentOfType<HttpVariable>() ?: return null
            else                           -> return null
        }

        val httpRequestTargetElement = convertedElement.parentOfType<HttpRequestTarget>() ?: return null
        if (convertedElement !== httpRequestTargetElement.firstChild) return null

        val httpRequestLineElement = httpRequestTargetElement.parentOfType<HttpRequestLine>() ?: return null
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