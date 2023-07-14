package com.esprito.spring.data.reference

import com.esprito.jpa.ql.psi.JpqlInputParameterExpression
import com.esprito.jpa.ql.reference.InputParameterReferenceResolver
import com.esprito.spring.data.SpringDataClasses
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import org.jetbrains.uast.*

class RepositoryQueryInputParameterReferenceResolver(
    project: Project
) : InputParameterReferenceResolver {
    private val injectedLanguageManager = InjectedLanguageManager.getInstance(project)
    override fun resolve(identifier: JpqlInputParameterExpression): List<ResolveResult> {
        val namedInputParameter = identifier.namedInputParameter
        if (namedInputParameter != null) {
            return resolveNamed(namedInputParameter)
        }

        val numericInputParameter = identifier.numericInputParameter
        if (numericInputParameter != null) {
            return resolveNumeric(numericInputParameter)
        }

        return emptyList()
    }

    private fun resolveNamed(
        namedInputParameter: PsiElement,
    ): List<ResolveResult> {
        val name = namedInputParameter.text?.substring(1)
            ?: return emptyList()

        val injectionHost = injectedLanguageManager.getInjectionHost(namedInputParameter)
            ?: return emptyList()

        val queryAnnotation = injectionHost.toUElement()
            ?.getParentOfType<UAnnotation>()
            ?.takeIf { it.qualifiedName == SpringDataClasses.QUERY }
            ?: return emptyList()

        val queryMethod = queryAnnotation
            .getParentOfType<UMethod>()
            ?: return emptyList()

        return queryMethod.uastParameters
            .filter {
                getParameterName(it) == name
            }.mapNotNull {
                it.sourcePsiElement
            }.map {
                PsiElementResolveResult(it)
            }
    }

    private fun resolveNumeric(numericInputParameter: PsiElement): List<ResolveResult> {
        val index = numericInputParameter.text?.substring(1)?.toInt()
            ?: return emptyList()

        if (index <= 0)
            return emptyList()

        val injectionHost = injectedLanguageManager.getInjectionHost(numericInputParameter)
            ?: return emptyList()

        val queryAnnotation = injectionHost.toUElement()
            ?.getParentOfType<UAnnotation>()
            ?.takeIf { it.qualifiedName == SpringDataClasses.QUERY }
            ?: return emptyList()

        val queryMethod = queryAnnotation
            .getParentOfType<UMethod>()
            ?: return emptyList()

        return listOfNotNull<ResolveResult>(
            queryMethod.uastParameters
                .getOrNull(index - 1) // start from 1
                ?.sourcePsiElement
                ?.let(::PsiElementResolveResult)
        )
    }

    private fun getParameterName(parameter: UParameter): String {
        return parameter.findAnnotation(SpringDataClasses.PARAM)
            ?.findAttributeValue("value")
            ?.evaluateString()
            ?: parameter.name
    }
}