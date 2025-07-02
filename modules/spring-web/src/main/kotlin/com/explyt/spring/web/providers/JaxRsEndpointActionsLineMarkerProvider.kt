package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.WebEeClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
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
        result += elements.mapNotNull { getLineMarkerFor(it) }
    }

    private fun getLineMarkerFor(psiElement: PsiElement): LineMarkerInfo<PsiElement>? {
        ProgressManager.checkCanceled()

        val uMethod = getUParentForIdentifier(psiElement) as? UMethod ?: return null
        val psiMethod = uMethod.javaPsi

        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null

        val httpMethodTargetClass = WebEeClasses.JAX_RS_HTTP_METHOD.getTargetClass(module)
        if (!psiMethod.isMetaAnnotatedBy(httpMethodTargetClass)) return null
        val psiClass = psiMethod.containingClass ?: return null
        val className = psiClass.name ?: return null

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

        val produces = getJaxRsProduces(psiMethod, module)
        val consumes = getJaxRsConsumes(psiMethod, module)

        val fullPath = SpringWebUtil.simplifyUrl("$applicationPath/$prefix/$path")

        val requestMethods = getJaxRsHttpMethods(psiMethod, module)
        if (requestMethods.isEmpty()) return null

        val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
        val returnType = uMethod.returnType
        val returnTypeFqn = SpringWebUtil.getTypeFqn(returnType, psiMethod.language)

        val argumentInfos = collectJaxRsArgumentInfos(psiMethod, module)

        val endpointElement = EndpointInfo(
            fullPath,
            requestMethods,
            psiElement,
            uMethod.name,
            className.decapitalize(),
            description,
            returnTypeFqn,
            argumentInfos.pathParameters,
            argumentInfos.queryParameters,
            null,
            argumentInfos.headerParameters,
            produces,
            consumes
        )

        return LineMarkerInfo(
            psiElement,
            psiElement.textRange,
            SpringIcons.ReadAccess,
            { SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.tooltip") },
            EndpointIconGutterHandler(endpointElement),
            GutterIconRenderer.Alignment.RIGHT,
            { SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.icon.accessible") }
        )
    }

}