package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytUastUtil.getCommentText
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lombok.utils.decapitalize
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class HttpExchangeEndpointActionsLineMarkerProvider : LineMarkerProviderDescriptor() {

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

        if (!psiMethod.isMetaAnnotatedBy(SpringWebClasses.HTTP_EXCHANGE)) return null
        val psiClass = psiMethod.containingClass ?: return null
        val className = psiClass.name ?: return null

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.HTTP_EXCHANGE)
        val path = requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("path", "value")).asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull() ?: ""
        if (OpenApiUtils.isAbsolutePath(path)) return null

        val prefix = if (psiClass.isMetaAnnotatedBy(SpringWebClasses.HTTP_EXCHANGE)) {
            requestMappingMah.getAnnotationMemberValues(psiClass, setOf("path", "value")).asSequence()
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .firstOrNull() ?: ""
        } else {
            ""
        }
        if (OpenApiUtils.isAbsolutePath(prefix)) return null

        val fullPath = SpringWebUtil.simplifyUrl("$prefix/$path")

        val requestMethods = requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("method"))
            .map { ExplytPsiUtil.getUnquotedText(it) }
        if (requestMethods.isEmpty()) return null

        val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
        val returnType = uMethod.returnType
        val returnTypeFqn = SpringWebUtil.getTypeFqn(returnType, psiMethod.language)

        val endpointElement = EndpointInfo(
            fullPath,
            requestMethods,
            psiElement,
            uMethod.name,
            className.decapitalize(),
            description,
            returnTypeFqn,
            SpringWebUtil.collectPathVariables(psiMethod),
            SpringWebUtil.collectRequestParameters(psiMethod),
            SpringWebUtil.getRequestBodyInfo(psiMethod),
            SpringWebUtil.collectRequestHeaders(psiMethod)
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