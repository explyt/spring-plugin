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

package com.explyt.spring.core.service

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses.ANNOTATION_CONFIG_CONTEXT
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInspection.isInheritorOf
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.util.Query
import org.jetbrains.uast.*

object AnnotationConfigApplicationService {
    fun getRootClasses(
        searchRootClasses: List<PsiClass>, rootModuleData: List<PackageScanService.ModuleRootData>
    ): List<PsiClass> {
        if (!(searchRootClasses.size == 1 && rootModuleData.isEmpty()
                    && Registry.`is`("explyt.spring.root.runConfiguration"))
        ) {
            return emptyList()
        }
        val mainClass = searchRootClasses[0]
        val module = ModuleUtilCore.findModuleForPsiElement(mainClass) ?: return emptyList()
        val annotationContext = LibraryClassCache
            .searchForLibraryClass(module, ANNOTATION_CONFIG_CONTEXT).toUElement() as? UClass
            ?: return emptyList()

        val findRegisterMethods = findRegisterClassMethod(annotationContext)
        val findConstructors = findConstructorClassMethod(annotationContext)
        val allMethods = findRegisterMethods + findConstructors

        return findComponentClasses(allMethods, mainClass, module.moduleScope)
    }

    private fun findRegisterClassMethod(annotationContext: UClass): List<UMethod> {
        return annotationContext.methods.asSequence()
            .filter { it.name == "register" }
            .filter { filterByParameterClass(it) }
            .toList()
    }

    private fun findConstructorClassMethod(annotationContext: UClass): List<UMethod> {
        return annotationContext.methods.asSequence()
            .filter { it.name == "AnnotationConfigApplicationContext" }
            .filter { filterByParameterClass(it) }
            .toList()
    }

    private fun findComponentClasses(
        uMethods: List<UMethod>, mainClass: PsiClass, moduleScope: GlobalSearchScope
    ): List<PsiClass> {
        return uMethods.asSequence()
            .flatMap { psiReferences(it, moduleScope) }
            .filter { it.element.containingFile.virtualFile == mainClass.containingFile.virtualFile }
            .mapNotNull { resolveConfigClass(it) }
            .distinct().toList()
    }

    private fun psiReferences(it: UMethod, moduleScope: GlobalSearchScope): Query<PsiReference> {
        val search = MethodReferencesSearch.search(it.javaPsi, moduleScope, false)
        return search
    }

    private fun resolveConfigClass(it: PsiReference): PsiClass? {
        val uCallExpression = it.element.getUastParentOfType<UCallExpression>() ?: return null
        if (uCallExpression.valueArgumentCount != 1) return null
        return when (val uExpression = uCallExpression.valueArguments[0]) {
            is UClassLiteralExpression -> uExpression.type?.resolvedPsiClass

            is UQualifiedReferenceExpression ->
                (uExpression.receiver as? UClassLiteralExpression)?.type?.resolvedPsiClass

            else -> null
        }
    }

    private fun filterByParameterClass(it: UMethod): Boolean {
        if (it.uastParameters.size != 1) return false
        return it.uastParameters[0].typeReference?.type?.deepComponentType
            ?.isInheritorOf(Class::class.java.canonicalName) == true
    }
}