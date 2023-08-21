package com.esprito.spring.core.profile

import com.esprito.spring.core.SpringCoreClasses
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiConstantEvaluationHelper
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import kotlin.streams.asSequence

class AnnotationProfileSearcher(
    private val project: Project
) : ProfileSearcher {

    private val javaPsiFacade by lazy {
        JavaPsiFacade.getInstance(project)
    }

    private val psiConstantEvaluationHelper: PsiConstantEvaluationHelper
        get() = javaPsiFacade.constantEvaluationHelper


    override fun searchProfiles(module: Module): List<String> {
        val profileAnnotations: Collection<PsiClass> = MetaAnnotationUtil
            .getAnnotationTypesWithChildren(
                module,
                SpringCoreClasses.PROFILE,
                false
            )

        return profileAnnotations.asSequence()
            .flatMap { metaProfileClass ->
                loadProfiles(metaProfileClass, module)
            }
            .distinct()
            .toList()
    }

    private fun maybeParseExpression(maybeExpression: String): List<String> {
        if (maybeExpression.contains(profileExpressionSymbols)) {
            return maybeExpression.split(profileExpressionSymbols).map {
                it.trim()
            }
        }

        return listOf(maybeExpression)
    }

    private fun loadProfiles(
        metaAnnotationClass: PsiClass,
        module: Module
    ): Sequence<String> {
        return AnnotatedElementsSearch.searchPsiClasses(metaAnnotationClass, module.moduleScope)
            .asSequence()
            .filter {
                !it.isAnnotationType
            }.flatMap {
                MetaAnnotationUtil.findMetaAnnotations(it, listOf(SpringCoreClasses.PROFILE))
                    .asSequence()
            }.flatMap {
                val attributeValue = it.findAttributeValue("value")
                    ?: return@flatMap sequenceOf()

                if (attributeValue is PsiArrayInitializerMemberValue) {
                    attributeValue.initializers.map(psiConstantEvaluationHelper::computeConstantExpression)
                        .asSequence()
                } else {
                    sequenceOf(psiConstantEvaluationHelper.computeConstantExpression(attributeValue))
                }

            }.filterIsInstance<String>()
            .flatMap { maybeParseExpression(it) }
    }

    companion object {
        private val profileExpressionSymbols = Regex("[|!&()]")
    }
}