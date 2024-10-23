package com.esprito.spring.web.providers

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.web.SpringWebBundle
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.providers.EndpointUsageSearcher.findMockMvcEndpointUsage
import com.esprito.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonEndpoints
import com.esprito.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlEndpoints
import com.esprito.spring.web.providers.EndpointUsageSearcher.findWebTestClientEndpointUsage
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

open class RouteFunctionEndpointLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uParent = getUParentForIdentifier(element)

        val parameter = if (element.language == KotlinLanguage.INSTANCE) getCoRouteFunction(uParent)
        else getRouteFunction(element)

        if (parameter == null) return
        val (path, methodNames) = parameter

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        result += NavigationGutterIconBuilder.create(SpringIcons.ReadAccess)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(//NotNullLazyValue.lazy {
                findOpenApiJsonEndpoints(path, listOf(methodNames), module) +
                        findOpenApiYamlEndpoints(path, listOf(methodNames), module) +
                        findMockMvcEndpointUsage(path, listOf(methodNames), module) +
                        findWebTestClientEndpointUsage(path, methodNames, module)
            )
            //})
            .setTargetRenderer { SpringWebUtil.getTargetRenderer() }
            .setTooltipText(SpringWebBundle.message("esprito.spring.web.gutter.endpoint.tooltip"))
            .setPopupTitle(SpringWebBundle.message("esprito.spring.web.gutter.endpoint.popup"))
            .setEmptyPopupText(SpringWebBundle.message("esprito.spring.web.gutter.endpoint.empty"))
            .createLineMarkerInfo(element)
    }

    private fun getRouteFunction(element: PsiElement): EndpointParameter? {
        val identifier = element as? PsiIdentifier ?: return null
        val methodName = identifier.text ?: return null
        if (methodName !in SpringWebClasses.URI_TYPE) return null

        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return null
        if (!method.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return null
        val psiClass = method.containingClass ?: return null
        if (!psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) return null

        val psiMethod = element.parentOfType<PsiMethodCallExpression>() ?: return null
        val path = getPathFromRouteFunction(psiMethod.toUElement())
        if (path.isEmpty()) return null

        return EndpointParameter(path, methodName)
    }

    private fun getCoRouteFunction(uParent: UElement?): EndpointParameter? {
        val uElement = uParent as? UCallExpression ?: return null

        val methodName = uElement.methodName ?: return null
        if (methodName !in SpringWebClasses.URI_TYPE) return null

        val method = findContainingMethod(uElement) ?: return null
        if (!method.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return null
        val psiClass = method.javaPsi.containingClass ?: return null
        if (!psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) return null

        val path = SpringWebUtil.getPathFromCallExpression(uElement)
        if (path.isEmpty()) return null

        return EndpointParameter(path, methodName)
    }

    private fun getPathFromRouteFunction(uMethod: UElement?): String {
        var path = ""
        uMethod?.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (node.methodName in SpringWebClasses.URI_TYPE) {
                    val uriArgument = node.valueArguments.getOrNull(0) as? ULiteralExpression
                    path = uriArgument?.value as? String ?: return super.visitCallExpression(node)
                }
                return super.visitCallExpression(node)
            }
        })
        return path
    }

    private fun findContainingMethod(expression: UElement): UMethod? {
        var currentExpression: UElement? = expression
        while (currentExpression != null && currentExpression !is UMethod) {
            currentExpression = currentExpression.uastParent
        }
        return currentExpression as? UMethod
    }

    data class EndpointParameter(
        val path: String,
        val methodNames: String
    )

}