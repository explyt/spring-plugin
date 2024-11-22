/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.references

import com.explyt.base.LibraryClassCache.searchForLibraryClass
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.searches.MethodReferencesSearch
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.getUastParentOfType


class SpringScopeReference(element: PsiLanguageInjectionHost) : PsiReferenceBase<PsiLanguageInjectionHost>(element) {

    override fun resolve() = element

    override fun getVariants(): Array<out Any> {
        val defaultScopes = listOf("singleton", "prototype")
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return getLookupArray(defaultScopes)


        val customScopes = ArrayList<String>()
        if (searchForLibraryClass(module, GENERIC_PORTLET_BEAN) != null) {
            customScopes.add("globalSession")
        }
        if (searchForLibraryClass(module, MVC_DISPATCHER_SERVLET) != null) {
            customScopes.add("request")
            customScopes.add("session")
        }
        if (searchForLibraryClass(module, BATCH_JOB_SCOPE) != null) {
            customScopes.add("job")
        }
        if (searchForLibraryClass(module, BATCH_STEP_SCOPE) != null) {
            customScopes.add("step")
        }
        customScopes += findCustomScopes(module)

        return getLookupArray(defaultScopes + customScopes)
    }

    private fun findCustomScopes(module: Module): List<String> {
        val psiClass = searchForLibraryClass(module, CONFIGURABLE_BEAN_FACTORY) ?: return emptyList()
        val registerScopePsiMethod = psiClass.findMethodsByName("registerScope", false).first() ?: return emptyList()
        val scope = module.moduleWithDependenciesScope
        val psiMethodReferences = MethodReferencesSearch.search(registerScopePsiMethod, scope, true)

        return psiMethodReferences.asSequence()
            .mapNotNull { it.element.getUastParentOfType<UCallExpression>() }
            .filter { it.valueArgumentCount == 2 }
            .mapNotNull { getExpressionValue(it.valueArguments[0]) }
            .toList()
    }

    private fun getExpressionValue(scopeNameUExpression: UExpression): String? {
        val sourcePsi = scopeNameUExpression.sourcePsi ?: return null
        if (scopeNameUExpression is ULiteralExpression) {
            return ElementManipulators.getValueText(sourcePsi)
        }
        val evaluationHelper = JavaPsiFacade.getInstance(sourcePsi.project).constantEvaluationHelper
        val constValue = evaluationHelper.computeConstantExpression(sourcePsi)
        return if (constValue is String) constValue else null
    }

    private fun getLookupArray(scopes: List<String>): Array<LookupElementBuilder> {
        return scopes.map { getLookupElement(it) }.toTypedArray()
    }

    private fun getLookupElement(scope: String): LookupElementBuilder {
        return LookupElementBuilder.create(scope)
    }

    companion object {
        private const val GENERIC_PORTLET_BEAN = "org.springframework.web.portlet.GenericPortletBean"
        private const val MVC_DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet"
        private const val BATCH_JOB_SCOPE = "org.springframework.batch.core.scope.JobScope"
        private const val BATCH_STEP_SCOPE = "org.springframework.batch.core.scope.StepScope"

        private const val CONFIGURABLE_BEAN_FACTORY = "org.springframework.beans.factory.config.ConfigurableBeanFactory"
    }
}