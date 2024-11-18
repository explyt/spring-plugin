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

package com.explyt.spring.core.search

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.uast.UastModificationTracker
import java.util.regex.Pattern

@Service(Service.Level.PROJECT)
class PsiPackageFqnSearchService(private val project: Project) {

    fun getAll(): Collection<String> {
        return fromProject() + fromLibraries()
    }

    fun fromProject(): Collection<String> {
        return synchronized(Monitor.Project) {
            CachedValuesManager.getManager(project)
                .getCachedValue(project) {
                    CachedValueProvider.Result(
                        doGet(ProjectScope.getContentScope(project)),
                        UastModificationTracker.getInstance(project)
                    )
                }
        }
    }

    fun fromLibraries(): Collection<String> {
        return synchronized(Monitor.Libraries) {
            CachedValuesManager.getManager(project)
                .getCachedValue(project) {
                    CachedValueProvider.Result(
                        doGet(ProjectScope.getLibrariesScope(project)),
                        UastModificationTracker.getInstance(project)
                    )
                }
        }
    }

    fun isPackageExist(antPattern: String): Boolean {
        return isPackageIn(antPattern, getAll())
    }

    fun isPackageIn(antPattern: String, packagesFqn: Collection<String>): Boolean {
        val regex = antPattern
            .replace("?", "\\w")
            .replace("**", "[\\w.]+")
            .replace("*", "[^.]*")
        val pattern = Pattern.compile(regex)

        return packagesFqn.any { pattern.matcher(it).matches() }
    }

    private fun doGet(scope: GlobalSearchScope): Collection<String> {
        val rootPackage = JavaPsiFacade.getInstance(project)
            .findPackage("") ?: return emptyList()
        val result = mutableSetOf<String>()

        processSubPackages(rootPackage, scope, result)
        return result
    }

    private fun processSubPackages(
        psiPackage: PsiPackage,
        scope: GlobalSearchScope,
        result: MutableSet<String>
    ) {
        for (subPackage in psiPackage.getSubPackages(scope)) {
            result.add(subPackage.qualifiedName)
            processSubPackages(subPackage, scope, result)
        }
    }

    private enum class Monitor {
        Project,
        Libraries
    }

    companion object {
        fun getInstance(project: Project): PsiPackageFqnSearchService = project.service()
    }

}