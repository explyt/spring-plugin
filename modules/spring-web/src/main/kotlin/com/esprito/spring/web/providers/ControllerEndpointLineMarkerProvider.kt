package com.esprito.spring.web.providers

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.web.SpringWebBundle
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.providers.EndpointUsageSearcher.findMockMvcEndpointUsage
import com.esprito.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonEndpoints
import com.esprito.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlEndpoints
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class ControllerEndpointLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uParent = getUParentForIdentifier(element)
        if (uParent !is UMethod) return
        val psiMethod = uParent.javaPsi

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return

        if (!psiMethod.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) return
        val psiClass = psiMethod.containingClass ?: return
        if (!psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)) return

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val path = requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("path", "value")).asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull() ?: ""

        val prefix = if (psiClass.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) {
            requestMappingMah.getAnnotationMemberValues(psiClass, setOf("path", "value")).asSequence()
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .firstOrNull() ?: ""
        } else {
            ""
        }

        val fullPath = SpringWebUtil.simplifyUrl("$prefix/$path")

        val requestMethods =
            requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("method"))
                .map { it.text.split('.').last() }

        result += NavigationGutterIconBuilder.create(SpringIcons.ReadAccess)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy {
                findOpenApiJsonEndpoints(fullPath, requestMethods, module) +
                        findOpenApiYamlEndpoints(fullPath, requestMethods, module) +
                        findMockMvcEndpointUsage(fullPath, requestMethods, module)
            })
            .setTargetRenderer { SpringWebUtil.getTargetRenderer() }
            .setTooltipText(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.tooltip"))
            .setPopupTitle(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.popup"))
            .setEmptyPopupText(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.empty"))
            .createLineMarkerInfo(element)
    }

}