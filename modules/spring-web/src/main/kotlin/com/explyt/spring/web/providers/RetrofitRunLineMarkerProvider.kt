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

import com.explyt.spring.web.editor.openapi.OpenApiUtils.getServerFromPath
import com.explyt.spring.web.editor.openapi.OpenApiUtils.isAbsolutePath
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.util.OpenApiFileUtil.Companion.DEFAULT_SERVER
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.collectRetrofitArgumentInfos
import com.explyt.spring.web.util.SpringWebUtil.getRetrofitHttpMethod
import com.explyt.spring.web.util.SpringWebUtil.getTypeFqn
import com.explyt.spring.web.util.SpringWebUtil.removeParams
import com.explyt.spring.web.util.SpringWebUtil.simplifyUrl
import com.explyt.util.ExplytUastUtil.getCommentText
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class RetrofitRunLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(psiElement: PsiElement): Info? {
        if (!SpringWebUtil.isSpringWebProject(psiElement.project)) return null
        val uMethod = getUParentForIdentifier(psiElement) as? UMethod ?: return null

        val psiMethod = uMethod.javaPsi
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return null

        val (path, requestMethod) = getRetrofitHttpMethod(psiMethod, module) ?: return null
        val fullPath = if (isAbsolutePath(path)) path else "$DEFAULT_SERVER/$path"

        val serverPart = getServerFromPath(fullPath) ?: return null
        val server = if (serverPart.startsWith('/')) serverPart.substring(1) else serverPart
        val apiPart = if (serverPart.length == fullPath.length) "/" else fullPath.substring(serverPart.length)

        val endpointInfo = getMethodEndpointInfo(uMethod, module, apiPart, requestMethod) ?: return null

        return Info(
            RunInSwaggerAction(listOf(endpointInfo), listOf(server))
        )
    }

    private fun getMethodEndpointInfo(
        uMethod: UMethod,
        module: Module,
        apiPath: String,
        requestMethod: String
    ): EndpointInfo? {
        ProgressManager.checkCanceled()

        val psiMethod = uMethod.javaPsi

        val requestMethods = listOf(requestMethod)
        if (requestMethods.isEmpty()) return null

        val fullPath = simplifyUrl(removeParams(apiPath))
        val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
        val returnType = uMethod.returnType
        val returnTypeFqn = getTypeFqn(returnType, psiMethod.language)

        val argumentInfos = collectRetrofitArgumentInfos(psiMethod, module)

        return EndpointInfo(
            fullPath,
            requestMethods,
            psiMethod,
            uMethod.name,
            "default",
            description,
            returnTypeFqn,
            argumentInfos.pathParameters,
            argumentInfos.queryParameters,
            null,
            argumentInfos.headerParameters
        )
    }

}