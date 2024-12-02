package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytUastUtil.getCommentText
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lombok.utils.decapitalize
import org.jetbrains.uast.*

class CoRouterEndpointActionsLineMarkerProvider : LineMarkerProviderDescriptor() {

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

        if (psiElement.language != KotlinLanguage.INSTANCE) return null

        val uCallExpression = getUParentForIdentifier(psiElement) as? UCallExpression ?: return null
        val methodName = uCallExpression.methodName ?: return null
        if (methodName !in SpringWebClasses.URI_TYPE) return null

        val method = findContainingMethod(uCallExpression) ?: return null
        if (!method.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return null
        val psiClass = method.javaPsi.containingClass ?: return null
        if (!psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) return null

        val fullPath = SpringWebUtil.getPathFromCallExpression(uCallExpression)
        if (fullPath.isEmpty()) return null

        val requestMethods = listOf(methodName)

        val description = uCallExpression.comments.firstOrNull()?.getCommentText() ?: ""
        val returnTypeFqn = "java.lang.String"
        val endpointMethodName = getEndpointMethodName(uCallExpression)
        val tag = method.name.decapitalize()

        val endpointElement = EndpointInfo(
            fullPath,
            requestMethods,
            psiElement,
            endpointMethodName,
            tag,
            description,
            returnTypeFqn
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

    private fun getEndpointMethodName(uCallExpression: UCallExpression): String {
        val methodReference = uCallExpression.valueArguments.getOrNull(1) as? UCallableReferenceExpression ?: return ""

        return methodReference.callableName
    }

    private fun findContainingMethod(expression: UElement): UMethod? {
        var currentExpression: UElement? = expression
        while (currentExpression != null && currentExpression !is UMethod) {
            currentExpression = currentExpression.uastParent
        }
        return currentExpression as? UMethod
    }

}