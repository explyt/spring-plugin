package com.esprito.spring.web.references.contributors.webClient

import com.esprito.spring.web.service.SpringWebEndpointsSearcher
import com.esprito.spring.web.util.SpringWebUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.tryResolve

data class UriWithEndpointTypes(val uri: String, val endpointTypes: Sequence<PsiClassReferenceType>) {
    companion object {
        fun endpointsTypesForExpression(
            referenceExpression: UQualifiedReferenceExpression?,
            responseSpec: String,
            uriSpec: String,
            module: Module
        ): UriWithEndpointTypes? {
            var receiver = referenceExpression
            if (receiver?.getExpressionType()?.canonicalText != responseSpec) return null

            var uri: String? = null
            var method: String? = null

            while (receiver != null && (uri == null || method == null)) {
                val psiMethod = receiver.tryResolve() as? PsiMethod
                if (psiMethod != null) {
                    val methodName = psiMethod.name
                    if (method == null && methodName in SpringWebUtil.REQUEST_METHODS) {
                        method = methodName.uppercase()
                    }

                    if (uri == null && methodName == "uri" && psiMethod.containingClass?.qualifiedName == uriSpec) {
                        uri = (receiver.selector as? UCallExpression)
                            ?.getArgumentForParameter(0)
                            ?.evaluateString()
                    }
                }
                receiver = receiver.receiver as? UQualifiedReferenceExpression
            }
            if (uri == null || method == null) return null

            val endpointTypes = SpringWebEndpointsSearcher.getInstance(module.project)
                .getAllEndpointElements(uri.replace("\"", ""), module)
                .asSequence()
                .filter { it.requestMethods.contains(method) }
                .mapNotNull { it.psiElement as? PsiMethod }
                .mapNotNull { it.returnType as? PsiClassReferenceType }

            return UriWithEndpointTypes(uri, endpointTypes)
        }

    }
}
