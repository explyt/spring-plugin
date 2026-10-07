/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils.getServerFromPath
import com.explyt.spring.web.editor.openapi.OpenApiUtils.isAbsolutePath
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.util.OpenApiFileUtil.Companion.DEFAULT_SERVER
import com.explyt.spring.web.util.ApplicationBasePath
import com.explyt.spring.web.util.HandlerMethods
import com.explyt.spring.web.util.HandlerSignature
import com.explyt.spring.web.util.MappingPathPlaceholders
import com.explyt.spring.web.util.OpenApiFileUtil.Companion.DEFAULT_SERVER_HOST
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.removeParams
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytUastUtil.getCommentText
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class EndpointRunLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(psiElement: PsiElement): Info? {
        val uMethod = getUParentForIdentifier(psiElement) as? UMethod ?: return null
        val psiMethod = uMethod.javaPsi

        if (!SpringWebUtil.isSpringWebProject(psiElement.project)) return null
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null

        if (!psiMethod.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) {
            val containingClass = psiMethod.containingClass ?: return null
            if (!SpringWebUtil.isRequestHandlerClass(containingClass)) return null
        }
        val mappingSource = SpringWebUtil.requestMappingSourceOf(psiMethod) ?: return null

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)

        val path = MappingPathPlaceholders.resolve(module, getUrlPath(requestMappingMah, psiMethod, mappingSource))

        val fullPath = if (isAbsolutePath(path)) path else "$DEFAULT_SERVER/$path"

        val serverPart = getServerFromPath(fullPath) ?: return null
        val server = if (serverPart.startsWith('/')) serverPart.substring(1) else serverPart
        val apiPart = if (serverPart.length == fullPath.length) "/" else fullPath.substring(serverPart.length)

        val endpointInfo = getEndpointInfo(uMethod, mappingSource, apiPart) ?: return null

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            arrayOf(RunInSwaggerAction(listOf(endpointInfo), listOf(server))),
            { SpringWebBundle.message("explyt.web.run.linemarker.swagger.title") }
        )
    }

    private fun getUrlPath(
        requestMappingMah: MetaAnnotationsHolder,
        psiMethod: PsiMethod,
        mappingSource: PsiMethod,
    ): String {
        var path = requestMappingMah.getAnnotationMemberValues(mappingSource, setOf("path", "value")).asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull() ?: ""
        if (isAbsolutePath(path)) return path

        val containingClass = psiMethod.containingClass ?: return path
        val prefix = HandlerMethods.requestMappingPrefixes(containingClass, requestMappingMah).firstOrNull() ?: ""
        if (prefix.isNotEmpty()) {
            path = SpringWebUtil.simplifyUrl("$prefix/$path")
            path = if (path.startsWith('/')) path.substring(1) else path
        }
        return path
    }

    private fun getEndpointInfo(uMethod: UMethod, mappingSource: PsiMethod, apiPath: String): EndpointInfo? {
        ProgressManager.checkCanceled()

        val psiMethod = uMethod.javaPsi

        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return null


        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val produces = requestMappingMah.getAnnotationMemberValues(mappingSource, setOf("produces"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
        val consumes = requestMappingMah.getAnnotationMemberValues(mappingSource, setOf("consumes"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }

        val fullPath = SpringWebUtil.simplifyUrl(removeParams(apiPath))

        val requestMethods =
            requestMappingMah.getAnnotationMemberValues(mappingSource, setOf("method"))
                .map { it.text.split('.').last() }

        val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
        val returnTypeFqn = SpringWebUtil.getTypeFqn(HandlerSignature.declaredReturnType(psiMethod), psiMethod.language)

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

    companion object {
        fun applyServerPortSettings(psiElement: PsiElement): List<String> {
            return applyServerPortSettings(psiElement, DEFAULT_SERVER)
        }

        /**
         * The servers a request to this module's endpoints goes to: `localhost` on each configured port, followed by
         * the base path the application declares, so a generated request reaches the path Spring serves.
         */
        private fun applyServerPortSettings(psiElement: PsiElement, server: String): List<String> {
            if (server != DEFAULT_SERVER) return listOf(server)
            val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return listOf(DEFAULT_SERVER)
            val basePath = ApplicationBasePath.of(module).orEmpty()

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
                .map { DEFAULT_SERVER_HOST + it + basePath }
                .distinct()
                .toList()
                .takeIf { it.isNotEmpty() } ?: listOf(DEFAULT_SERVER)
            return ports
        }

    }
}