/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.ai.mcp.PageSerialization.Companion.ENABLE_WEB_SUPPORT
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.web.util.ApplicationModules
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

/**
 * The `@EnableSpringDataWebSupport` classes the context serving a handler module can register.
 *
 * The serving modules are the handler module and every module depending on it that declares an application. Project
 * classes are [reached]; a library class is reached when a project `@Import` names it or a project component scan
 * covers its package. A library auto-configuration that is not reached may still be registered by Boot, so it is kept
 * apart in [unreachedAutoConfigurations]; any other library class never enters the context.
 */
internal class WebSupportDeclarations private constructor(
    val reached: List<PsiClass>,
    val unreachedAutoConfigurations: List<PsiClass>,
) {

    companion object {
        private val NONE = WebSupportDeclarations(emptyList(), emptyList())

        fun of(module: Module): WebSupportDeclarations {
            val serving = listOf(module) + ApplicationModules.dependentApplicationModules(module)
            val searched = serving.filter(::resolvesWebSupport)
            if (searched.isEmpty()) return NONE
            val project = module.project
            val projectClasses = annotatedIn(searched, ProjectScope.getProjectScope(project))
            val libraryClasses = annotatedIn(searched, ProjectScope.getLibrariesScope(project))
            if (libraryClasses.isEmpty()) return WebSupportDeclarations(projectClasses, emptyList())

            val (reachedLibraryClasses, unreached) = libraryClasses.partition(projectReachIn(serving))
            val importedByBoot by lazy { autoConfigurationNamesIn(serving) }
            return WebSupportDeclarations(
                (projectClasses + reachedLibraryClasses).sortedBy { it.qualifiedName },
                unreached.filter {
                    it.isMetaAnnotatedBy(SpringCoreClasses.BOOT_AUTO_CONFIGURATION) || it.qualifiedName in importedByBoot
                },
            )
        }

        private fun productionScopeOf(module: Module): GlobalSearchScope =
            GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)

        private fun resolvesWebSupport(module: Module): Boolean =
            JavaPsiFacade.getInstance(module.project).findClass(ENABLE_WEB_SUPPORT, productionScopeOf(module)) != null

        private fun annotatedIn(modules: List<Module>, origin: GlobalSearchScope): List<PsiClass> =
            modules.flatMap { module ->
                val scope = productionScopeOf(module).intersectWith(origin)
                MetaAnnotationUtil.getAnnotationTypesWithChildren(module, ENABLE_WEB_SUPPORT, false)
                    .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            }
                .filter { !it.isAnnotationType && it.qualifiedName != null }
                .distinctBy { it.qualifiedName }
                .sortedBy { it.qualifiedName }

        private fun projectReachIn(serving: List<Module>): (PsiClass) -> Boolean {
            val scan = PackageScanService.getInstance(serving.first().project).getAllPackages()
            val scannedPackages = serving.flatMapTo(mutableSetOf(), scan::getPackages)
            return { psiClass ->
                val fqn = psiClass.qualifiedName.orEmpty()
                fqn in scan.importQualified || scannedPackages.any(fqn::startsWith)
            }
        }

        private fun autoConfigurationNamesIn(serving: List<Module>): Set<String> {
            val search = SpringConfigurationPropertiesSearch.getInstance(serving.first().project)
            return serving.flatMapTo(mutableSetOf(), search::getAllFactoriesNames)
        }
    }
}
