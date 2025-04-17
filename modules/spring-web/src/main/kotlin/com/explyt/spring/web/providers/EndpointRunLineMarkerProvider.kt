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

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils.getServerFromPath
import com.explyt.spring.web.editor.openapi.OpenApiUtils.isAbsolutePath
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.util.OpenApiFileUtil.Companion.DEFAULT_SERVER
import com.explyt.spring.web.util.OpenApiFileUtil.Companion.DEFAULT_SERVER_HOST
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.removeParams
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytUastUtil.getCommentText
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class EndpointRunLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(psiElement: PsiElement): Info? {
        val uMethod = getUParentForIdentifier(psiElement) as? UMethod ?: return null
        val psiMethod = uMethod.javaPsi

        if (!SpringWebUtil.isSpringWebProject(psiElement.project)) return null
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null

        if (!psiMethod.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) return null

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val path = requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("path", "value")).asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull() ?: ""
        val fullPath = if (isAbsolutePath(path)) path else "$DEFAULT_SERVER/$path"

        val serverPart = getServerFromPath(fullPath) ?: return null
        val server = if (serverPart.startsWith('/')) serverPart.substring(1) else serverPart
        val apiPart = if (serverPart.length == fullPath.length) "/" else fullPath.substring(serverPart.length)

        val endpointInfo = getEndpointInfo(uMethod, apiPart) ?: return null
        val servers = applyServerPortSettings(psiElement, server)
        return Info(
            RunInSwaggerAction(listOf(endpointInfo), servers)
        )
    }

    private fun applyServerPortSettings(psiElement: PsiElement, server: String): List<String> {
        if (server != DEFAULT_SERVER) return listOf(server)
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return listOf(DEFAULT_SERVER)

        val ports = (DefinedConfigurationPropertiesSearch.getInstance(psiElement.project)
            .findProperties(module, "server.port").asSequence()
            .mapNotNull { it.value } + listOf("8080"))
            .map {
                if (it.contains("{")) {
                    it.substringAfter("{").substringBefore("}").substringAfter(":")
                } else {
                    it
                }
            }
            .map { DEFAULT_SERVER_HOST + it }
            .distinct()
            .toList()
            .takeIf { it.isNotEmpty() } ?: listOf(DEFAULT_SERVER)
        return ports
    }

    private fun getEndpointInfo(uMethod: UMethod, apiPath: String): EndpointInfo? {
        ProgressManager.checkCanceled()

        val psiMethod = uMethod.javaPsi

        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return null

        if (!psiMethod.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) return null

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val produces = requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("produces"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
        val consumes = requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("consumes"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }

        val fullPath = SpringWebUtil.simplifyUrl(removeParams(apiPath))

        val requestMethods =
            requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("method"))
                .map { it.text.split('.').last() }

        val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
        val returnType = uMethod.returnType
        val returnTypeFqn = SpringWebUtil.getTypeFqn(returnType, psiMethod.language)

        return EndpointInfo(
            fullPath,
            requestMethods,
            psiMethod,
            uMethod.name,
            "default",
            description,
            returnTypeFqn,
            SpringWebUtil.collectPathVariables(psiMethod),
            SpringWebUtil.collectRequestParameters(psiMethod),
            SpringWebUtil.getRequestBodyInfo(psiMethod),
            SpringWebUtil.collectRequestHeaders(psiMethod),
            produces,
            consumes
        )
    }

}