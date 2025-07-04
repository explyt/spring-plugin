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

import com.explyt.spring.core.util.SpringCoreUtil.isMapWithStringKey
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.WebEeClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils.getServerFromPath
import com.explyt.spring.web.editor.openapi.OpenApiUtils.isAbsolutePath
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.OpenApiFileUtil.Companion.DEFAULT_SERVER
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.PathArgumentInfo
import com.explyt.spring.web.util.SpringWebUtil.getTypeFqn
import com.explyt.spring.web.util.SpringWebUtil.simplifyUrl
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isOptional
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass
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
        if (!psiMethod.isMetaAnnotatedBy(WebEeClasses.JAX_RS_PATH.allFqns)
            && !psiMethod.isMetaAnnotatedBy(WebEeClasses.JAX_RS_HTTP_METHOD.allFqns)
        ) return null

        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return null
        val fullPath = SpringWebUtil.getJaxRsPaths(psiMethod, module).asSequence()
            .map { if (isAbsolutePath(it)) it else getFullPath(it, module, uMethod) }
            .firstOrNull() ?: return null

        val serverPart = getServerFromPath(fullPath) ?: return null
        val server = if (serverPart.startsWith('/')) serverPart.substring(1) else serverPart
        val apiPart = if (serverPart.length == fullPath.length) "/" else fullPath.substring(serverPart.length)
        val endpointInfo = JaxRsEndpointActionsLineMarkerProvider
            .getEndpointInfo(apiPart, uMethod, module) ?: return null

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            arrayOf(RunInSwaggerAction(listOf(endpointInfo), listOf(server))),
            { SpringWebBundle.message("explyt.web.run.linemarker.swagger.title") }
        )
    }

    private fun getFullPath(path: String, module: Module, uMethod: UMethod): String {
        val applicationPath = SpringWebEndpointsSearcher.getInstance(module.project).getJaxRsApplicationPath(module)
        val prefix = getClassPrefix(uMethod.getContainingUClass()?.javaPsi, module) ?: ""
        if (isAbsolutePath(prefix)) return simplifyUrl("$prefix/$path")
        val simplifyUrl = simplifyUrl("$applicationPath/$prefix/$path")
        val fullPath = if (simplifyUrl.startsWith('/')) simplifyUrl.substring(1) else simplifyUrl
        return "$DEFAULT_SERVER/$fullPath"
    }

    private fun getClassPrefix(javaPsiClass: PsiClass?, module: Module): String? {
        val pathTargetClass = WebEeClasses.JAX_RS_PATH.getTargetClass(module)
        return if (javaPsiClass != null && javaPsiClass.isMetaAnnotatedBy(pathTargetClass)) {
            SpringWebUtil.getJaxRsPaths(javaPsiClass, module).firstOrNull()
        } else null
    }

    private fun getInfo(uClass: UClass): Info? {
        val psiClass = uClass.javaPsi
        if (!psiClass.isMetaAnnotatedBy(WebEeClasses.JAX_RS_PATH.allFqns)) return null

        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null

        val jaxRsPaths = SpringWebUtil.getJaxRsPaths(psiClass, module)
        val servers = jaxRsPaths.filter { isAbsolutePath(it) }

        val endpointInfos = uClass.methods
            .mapNotNull { JaxRsEndpointActionsLineMarkerProvider.getEndpointInfo(it) }

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            arrayOf(RunInSwaggerAction(endpointInfos, servers)),
            { SpringWebBundle.message("explyt.web.run.linemarker.swagger.title") }
        )
    }

    companion object {
        const val VALUE = "value"

        fun getRequestBodyInfo(psiMethod: PsiMethod, requestMethods: List<String>): PathArgumentInfo? {
            if (requestMethods.size == 1 && requestMethods[0].lowercase() == "get") return null
            val bodyParams = psiMethod.parameterList.parameters.filter { it.annotations.isEmpty() }

            for (param in bodyParams) {
                val paramType = param.type
                val isMap = paramType.isMapWithStringKey()
                val isOptional = !isMap && paramType.isOptional
                val typeFqn = getTypeFqn(paramType, psiMethod.language)

                return PathArgumentInfo(
                    param.name,
                    param,
                    !isOptional,
                    isMap,
                    typeFqn
                )
            }
            return null
        }
    }

}