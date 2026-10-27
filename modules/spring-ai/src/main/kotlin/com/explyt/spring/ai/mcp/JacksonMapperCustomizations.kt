/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.JacksonClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil

/**
 * The places where a project configures its Jackson mapper in code, which Spring Boot's auto-configuration yields to.
 *
 * Boot declares its `ObjectMapper`/`JsonMapper` and the builder it is built from under `@ConditionalOnMissingBean`,
 * so a project bean of either type replaces them and `spring.jackson.*` no longer applies to it. Boot applies the
 * properties through a builder customizer bean at order 0, so a project customizer bean, which runs after it unless
 * ordered otherwise, can override them. What such code sets is not read; the contract reports the strategy as
 * unknown and names the declaration instead.
 *
 * Only production project code counts: Boot's own auto-configured mapper is the one the properties describe, and a
 * `@TestConfiguration` under a test source root never reaches the running application. A class implementing a
 * customizer interface counts whether or not it is registered as a bean - it can only take effect as one, and doubt
 * is the right answer - except a local or anonymous class, which no component scan can register.
 */
internal object JacksonMapperCustomizations {

    /** The declarations in [module] or a project module it depends on: mapper and builder beans first, each group sorted. */
    fun declaredIn(module: Module): List<String> =
        CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                find(module),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }

    private fun find(module: Module): List<String> {
        val production = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        val facade = JavaPsiFacade.getInstance(module.project)
        val replaceable = (JacksonClasses.OBJECT_MAPPERS + JacksonClasses.MAPPER_BUILDERS)
            .mapNotNull { facade.findClass(it, production) }
        val customizers = JacksonClasses.MAPPER_BUILDER_CUSTOMIZERS.mapNotNull { facade.findClass(it, production) }
        if (replaceable.isEmpty() && customizers.isEmpty()) return emptyList()

        val projectSources = production.intersectWith(ProjectScope.getProjectScope(module.project))
        val beans = beanMethodsIn(module, projectSources)
            .filter { method -> returnTypeOf(method)?.let { type -> inherits(type, replaceable + customizers) } == true }
            .map { "@Bean ${it.name} in ${it.containingClass?.qualifiedName}" }
            .sorted()
            .toList()
        val implementors = customizers.flatMap { customizer ->
            ClassInheritorsSearch.search(customizer, projectSources, true)
                .filter(::canBeRegistered)
                .map { "${it.qualifiedName} implements ${customizer.qualifiedName}" }
        }.sorted()
        return beans + implementors
    }

    private fun beanMethodsIn(module: Module, scope: GlobalSearchScope): Sequence<PsiMethod> =
        MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.BEAN, false)
            .asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiMethods(it, scope) }

    private fun canBeRegistered(psiClass: PsiClass): Boolean =
        !psiClass.isInterface &&
                !psiClass.hasModifierProperty(PsiModifier.ABSTRACT) &&
                !PsiUtil.isLocalOrAnonymousClass(psiClass)

    private fun returnTypeOf(method: PsiMethod): PsiClass? = (method.returnType as? PsiClassType)?.resolve()

    private fun inherits(type: PsiClass, bases: List<PsiClass>): Boolean =
        bases.any { InheritanceUtil.isInheritorOrSelf(type, it, true) }
}
