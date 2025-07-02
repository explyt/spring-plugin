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

import com.explyt.spring.web.WebEeClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils.getServerFromPath
import com.explyt.spring.web.editor.openapi.OpenApiUtils.isAbsolutePath
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.util.OpenApiFileUtil.Companion.DEFAULT_SERVER
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.collectJaxRsArgumentInfos
import com.explyt.spring.web.util.SpringWebUtil.getJaxRsConsumes
import com.explyt.spring.web.util.SpringWebUtil.getJaxRsHttpMethods
import com.explyt.spring.web.util.SpringWebUtil.getJaxRsProduces
import com.explyt.spring.web.util.SpringWebUtil.getTypeFqn
import com.explyt.spring.web.util.SpringWebUtil.removeParams
import com.explyt.spring.web.util.SpringWebUtil.simplifyUrl
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytUastUtil.getCommentText
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lombok.utils.decapitalize
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class JaxRsRunLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(psiElement: PsiElement): Info? {
        val uParent = getUParentForIdentifier(psiElement) ?: return null

        return when (uParent) {
            is UMethod -> getInfo(uParent)
            is UClass -> getInfo(uParent)
            else -> null
        }
    }

    private fun getInfo(uMethod: UMethod): Info? {
        val psiMethod = uMethod.javaPsi
        if (!psiMethod.isMetaAnnotatedBy(WebEeClasses.JAX_RS_PATH.allFqns)) return null

        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return null

        val path = SpringWebUtil.getJaxRsPaths(psiMethod, module).asSequence()
            .map { if (isAbsolutePath(it)) it else "$DEFAULT_SERVER/$it" }
            .firstOrNull() ?: return null

        val serverPart = getServerFromPath(path) ?: return null
        val server = if (serverPart.startsWith('/')) serverPart.substring(1) else serverPart
        val apiPart = if (serverPart.length == path.length) "/" else path.substring(serverPart.length)

        val endpointInfo = getMethodEndpointInfo(uMethod, module, apiPart) ?: return null

        return Info(
            RunInSwaggerAction(listOf(endpointInfo), listOf(server))
        )
    }

    private fun getInfo(uClass: UClass): Info? {
        val psiClass = uClass.javaPsi
        if (!psiClass.isMetaAnnotatedBy(WebEeClasses.JAX_RS_PATH.allFqns)) return null

        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null

        val servers = SpringWebUtil.getJaxRsPaths(psiClass, module)
            .filter { isAbsolutePath(it) }

        val prefix = servers.firstOrNull { !isAbsolutePath(it) } ?: ""
        val endpointInfos = uClass.methods
            .mapNotNull { getClassEndpointInfo(it, module, prefix) }

        return Info(
            RunInSwaggerAction(endpointInfos, servers)
        )

    }


    private fun getMethodEndpointInfo(uMethod: UMethod, module: Module, apiPath: String): EndpointInfo? {
        ProgressManager.checkCanceled()

        val psiMethod = uMethod.javaPsi

        val requestMethods = getJaxRsHttpMethods(psiMethod, module)
        if (requestMethods.isEmpty()) return null

        val produces = getJaxRsProduces(psiMethod, module)
        val consumes = getJaxRsConsumes(psiMethod, module)

        val fullPath = simplifyUrl(removeParams(apiPath))

        val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
        val returnType = uMethod.returnType
        val returnTypeFqn = getTypeFqn(returnType, psiMethod.language)

        val argumentInfos = collectJaxRsArgumentInfos(psiMethod, module)

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
            argumentInfos.headerParameters,

            produces,
            consumes
        )
    }

    private fun getClassEndpointInfo(uMethod: UMethod, module: Module, prefix: String = ""): EndpointInfo? {
        ProgressManager.checkCanceled()

        val psiMethod = uMethod.javaPsi

        if (!psiMethod.isMetaAnnotatedBy(WebEeClasses.JAX_RS_HTTP_METHOD.allFqns)) return null
        if (!psiMethod.isMetaAnnotatedBy(WebEeClasses.JAX_RS_PATH.allFqns)) return null
        val psiClass = psiMethod.containingClass ?: return null
        val controllerName = psiClass.name ?: return null

        val path = SpringWebUtil.getJaxRsPaths(psiMethod, module).asSequence()
            .filter { !isAbsolutePath(it) }
            .firstOrNull() ?: return null

        val produces = getJaxRsProduces(psiMethod, module)
        val consumes = getJaxRsConsumes(psiMethod, module)

        val fullPath = simplifyUrl("$prefix/${removeParams(path)}")

        val requestMethods = getJaxRsHttpMethods(psiMethod, module)
        if (requestMethods.isEmpty()) return null

        val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
        val returnType = uMethod.returnType
        val returnTypeFqn = getTypeFqn(returnType, psiMethod.language)

        val argumentInfos = collectJaxRsArgumentInfos(psiMethod, module)

        return EndpointInfo(
            fullPath,
            requestMethods,
            psiMethod,
            uMethod.name,
            controllerName.replace("controller", "", true)
                .decapitalize(),
            description,
            returnTypeFqn,
            argumentInfos.pathParameters,
            argumentInfos.queryParameters,
            null,
            argumentInfos.headerParameters,
            produces,
            consumes
        )
    }

    companion object {
        const val VALUE = "value"
    }

}