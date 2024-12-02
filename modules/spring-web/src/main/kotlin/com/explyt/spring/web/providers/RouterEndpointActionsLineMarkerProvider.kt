package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytUastUtil.getCommentText
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

class RouterEndpointActionsLineMarkerProvider : LineMarkerProviderDescriptor() {

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

        if (psiElement.language == KotlinLanguage.INSTANCE) return null

        val identifier = psiElement as? PsiIdentifier ?: return null
        val methodName = identifier.text ?: return null
        if (methodName !in SpringWebClasses.URI_TYPE) return null

        val routerFunction = psiElement.parentOfType<PsiMethod>() ?: return null
        if (!routerFunction.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return null
        val psiClass = routerFunction.containingClass ?: return null
        if (!psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) return null

        val psiMethodCall = psiElement.parentOfType<PsiMethodCallExpression>() ?: return null
        val uMethodCall = psiMethodCall.toUElementOfType<UCallExpression>() ?: return null
        val fullPath = getPathFromRouteFunction(uMethodCall)
        if (fullPath.isEmpty()) return null

        val requestMethods = listOf(methodName)

        val description = uMethodCall
            .comments
            .firstOrNull()
            ?.getCommentText() ?: ""
        val returnTypeFqn = "java.lang.String"
        val endpointMethodName = getEndpointMethodName(uMethodCall)

        val endpointElement = EndpointInfo(
            fullPath,
            requestMethods,
            psiElement,
            endpointMethodName,
            routerFunction.name,
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
        val methodReference = uCallExpression.valueArguments.getOrNull(2) as? UCallableReferenceExpression ?: return ""

        return methodReference.callableName
    }

    private fun getPathFromRouteFunction(uMethodCall: UCallExpression): String {
        var path = ""
        uMethodCall.accept(object : AbstractUastVisitor() {
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

}