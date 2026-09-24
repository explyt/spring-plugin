/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.service.PackageScanService
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

/**
 * Picks the one Spring application a bean query is scoped to.
 *
 * The choice is made from the project model alone - never from the selected editor or run configuration - so the
 * same request answers the same way regardless of what the user happens to have open. When the scope holds more
 * than one application the caller is asked to name it: two applications in one module are two contexts, and
 * silently answering from the first one is indistinguishable from answering from the right one.
 */
class BeanApplicationResolver(private val project: Project) {

    /**
     * Must run under a read action.
     *
     * @param injectionFile narrows the candidates to the applications whose module scope contains that file.
     */
    fun resolve(applicationClassName: String?, injectionFile: PsiFile?): PsiClass {
        val candidates = candidates(injectionFile)
        if (applicationClassName != null) return pickNamed(candidates, applicationClassName)

        return when (candidates.size) {
            1 -> candidates.single()
            0 -> throw BeanQueryException(
                BeanQueryProblem(APPLICATION_NOT_FOUND, "No Spring Boot application found in the scope.")
            )

            else -> throw BeanQueryException(
                BeanQueryProblem(
                    APPLICATION_REQUIRED,
                    "Choose one Spring Boot application.",
                    candidates.map { it.asChoice() }
                )
            )
        }
    }

    /**
     * A named application is looked up among the candidates rather than through a plain class search: a class that
     * exists but is not a Spring application, or is out of the injection file's scope, is a different mistake than
     * a missing one, and only the candidate list can tell them apart.
     */
    private fun pickNamed(candidates: List<PsiClass>, applicationClassName: String): PsiClass {
        val named = candidates.filter { it.qualifiedName == applicationClassName }
        return when (named.size) {
            1 -> named.single()
            0 -> throw BeanQueryException(
                BeanQueryProblem(
                    APPLICATION_NOT_FOUND,
                    "The requested class is not a Spring Boot application in the scope."
                )
            )

            // The same FQN declared in two module roots is an ambiguity the caller has to resolve; taking the
            // first one would answer from a module the caller never named.
            else -> throw BeanQueryException(
                BeanQueryProblem(
                    APPLICATION_REQUIRED,
                    "Several modules declare this application class.",
                    named.map { it.asChoice() }
                )
            )
        }
    }

    private fun candidates(injectionFile: PsiFile?): List<PsiClass> {
        val applications = PackageScanService.getInstance(project).getSpringBootAppAnnotations().asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
            .onEach { ProgressManager.checkCanceled() }
            .filter { it.isValid && it.qualifiedName != null }
            .distinctBy { it.qualifiedName to ModuleUtilCore.findModuleForPsiElement(it)?.name }
            .toList()

        injectionFile ?: return applications
        val injectionVirtualFile = injectionFile.virtualFile ?: return applications

        // The direction matters: an application is a candidate when **it** sees the file, not when the file's own
        // module happens to see the application. A module the injection file depends on is a library to it - its
        // application never scans the file - so the reverse test would make every upstream application a candidate.
        return applications.filter { application ->
            val applicationModule = ModuleUtilCore.findModuleForPsiElement(application) ?: return@filter false
            GlobalSearchScope.moduleWithDependenciesScope(applicationModule).contains(injectionVirtualFile)
        }
    }

    private fun PsiClass.asChoice(): Map<String, String> = buildMap {
        qualifiedName?.let { put("applicationClassName", it) }
        ModuleUtilCore.findModuleForPsiElement(this@asChoice)?.let { put("module", it.name) }
    }

    companion object {
        const val APPLICATION_NOT_FOUND = "APPLICATION_NOT_FOUND"
        const val APPLICATION_REQUIRED = "APPLICATION_REQUIRED"
    }
}
