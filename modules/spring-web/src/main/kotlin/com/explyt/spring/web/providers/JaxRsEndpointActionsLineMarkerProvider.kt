package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.WebEeClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.providers.JaxRsRunLineMarkerProvider.Companion.getRequestBodyInfo
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.collectJaxRsArgumentInfos
import com.explyt.spring.web.util.SpringWebUtil.getJaxRsConsumes
import com.explyt.spring.web.util.SpringWebUtil.getJaxRsHttpMethods
import com.explyt.spring.web.util.SpringWebUtil.getJaxRsProduces
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytUastUtil.getCommentText
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lombok.utils.decapitalize
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class JaxRsEndpointActionsLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getName(): String? = null
    override fun getLineMarkerInfo(element: PsiElement) = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        result += elements.mapNotNull {
            val uMethod = getUParentForIdentifier(it) as? UMethod ?: return@mapNotNull null
            val endpointInfo = getEndpointInfo(uMethod) ?: return@mapNotNull null
            getLineMarkerFor(endpointInfo, it)
        }
    }

    companion object {
        fun getEndpointInfo(uMethod: UMethod): EndpointInfo? {
            ProgressManager.checkCanceled()

            val psiMethod = uMethod.javaPsi

            val module = ModuleUtilCore.findModuleForPsiElement(uMethod.javaPsi) ?: return null

            val httpMethodTargetClass = WebEeClasses.JAX_RS_HTTP_METHOD.getTargetClass(module)
            if (!psiMethod.isMetaAnnotatedBy(httpMethodTargetClass)) return null
            val psiClass = psiMethod.containingClass ?: return null

            val path = SpringWebUtil.getJaxRsPaths(psiMethod, module).asSequence()
                .filter { !OpenApiUtils.isAbsolutePath(it) }
                .firstOrNull() ?: return null

            val pathTargetClass = WebEeClasses.JAX_RS_PATH.getTargetClass(module)
            val applicationPath = SpringWebEndpointsSearcher.getInstance(module.project).getJaxRsApplicationPath(module)
            val prefix = if (psiClass.isMetaAnnotatedBy(pathTargetClass)) {
                SpringWebUtil.getJaxRsPaths(psiClass, module)
                    .firstOrNull() ?: ""
            } else {
                ""
            }
            if (OpenApiUtils.isAbsolutePath(prefix)) return null

            val fullPath = SpringWebUtil.simplifyUrl("$applicationPath/$prefix/$path")

            return getEndpointInfo(fullPath, uMethod, module)
        }

        fun getEndpointInfo(fullPath: String, uMethod: UMethod, module: Module): EndpointInfo? {
            val psiMethod = uMethod.javaPsi
            val requestMethods = getJaxRsHttpMethods(psiMethod, module)
            if (requestMethods.isEmpty()) return null
            val className = psiMethod.containingClass?.name ?: return null

            val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
            val returnType = uMethod.returnType
            val returnTypeFqn = SpringWebUtil.getTypeFqn(returnType, psiMethod.language)

            val argumentInfos = collectJaxRsArgumentInfos(psiMethod, module)
            val produces = getJaxRsProduces(psiMethod, module)
            val consumes = getJaxRsConsumes(psiMethod, module)

            return EndpointInfo(
                fullPath,
                requestMethods,
                psiMethod,
                uMethod.name,
                className.decapitalize(),
                description,
                returnTypeFqn,
                argumentInfos.pathParameters,
                argumentInfos.queryParameters,
                getRequestBodyInfo(psiMethod, requestMethods),
                argumentInfos.headerParameters,
                produces,
                consumes
            )
        }

        private fun getLineMarkerFor(endpointInfo: EndpointInfo, psiElement: PsiElement): LineMarkerInfo<PsiElement?> {
            return LineMarkerInfo(
                psiElement,
                psiElement.textRange,
                SpringIcons.ReadAccess,
                { SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.tooltip") },
                EndpointIconGutterHandler(endpointInfo),
                GutterIconRenderer.Alignment.RIGHT,
                { SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.icon.accessible") }
            )
        }


    }

}