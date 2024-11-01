package com.explyt.spring.data.reference

import com.explyt.jpa.ql.psi.JpqlInputParameterExpression
import com.explyt.jpa.ql.reference.InputParameterReferenceResolver
import com.explyt.spring.core.SpringProperties.COLON
import com.explyt.spring.data.SpringDataClasses
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import org.jetbrains.uast.*

class RepositoryQueryInputParameterReferenceResolver(
    project: Project
) : InputParameterReferenceResolver {
    private val injectedLanguageManager = InjectedLanguageManager.getInstance(project)

    @Suppress("UElementAsPsi")
    override fun getVariants(identifier: JpqlInputParameterExpression): List<Any> {
        val namedInputParameter = identifier.namedInputParameter
        if (namedInputParameter != null) {
            return loadMethod(namedInputParameter)?.uastParameters?.map { COLON + it.name } ?: emptyList()
        }

        val numericInputParameter = identifier.numericInputParameter
        if (numericInputParameter != null) {
            val method = loadMethod(numericInputParameter) ?: return emptyList()

            return method
                .uastParameters.mapIndexed { index, uParameter ->
                    LookupElementBuilder.create("?${index + 1}")
                        .withTailText(" ${uParameter.name}", true)
                }
        }

        return emptyList()
    }

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

    private fun loadMethod(inputParameter: PsiElement): UMethod? {
        val injectionHost = injectedLanguageManager.getInjectionHost(inputParameter)
            ?: return null

        val queryAnnotation = injectionHost.toUElement()
            ?.getParentOfType<UAnnotation>()
            ?.takeIf { it.qualifiedName == SpringDataClasses.QUERY }
            ?: return null

        return queryAnnotation
            .getParentOfType<UMethod>()
    }

    private fun resolveNamed(
        namedInputParameter: PsiElement,
    ): List<ResolveResult> {
        val name = namedInputParameter.text?.substring(1)
            ?: return emptyList()

        val queryMethod = loadMethod(namedInputParameter) ?: return emptyList()

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

        val queryMethod = loadMethod(numericInputParameter) ?: return emptyList()

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