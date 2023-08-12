package com.esprito.spring.core.util

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope

object PsiPackagesSearcher {
    // TODO - support **
    fun getFilteredPackages(project: Project, scope: GlobalSearchScope, antFilter: String): List<PsiPackage> {
        val rootPackage = JavaPsiFacade.getInstance(project)
            .findPackage("")
            ?: return emptyList()

        val parts = antFilter.split(".")
        return collectPackages(rootPackage, scope, parts)
    }

    private fun collectPackages(
        parentPackage: PsiPackage,
        scope: GlobalSearchScope,
        rest: List<String>
    ): List<PsiPackage> {
        val currentPart = rest.firstOrNull()
            ?: return listOf(parentPackage)

        val childParts = rest.drop(1)

        val subPackages = parentPackage.getSubPackages(scope)

        return subPackages
            .filter { matches(it, currentPart) }
            .flatMap {
                collectPackages(it, scope, childParts)
            }
    }

    private fun matches(psiPackage: PsiPackage, namePattern: String): Boolean {
        if (namePattern == "*")
            return true

        val name = psiPackage.name ?: return false

        if (!namePattern.contains('?')) {
            return name == namePattern
        }

        return Regex(namePattern.replace("?", "\\w{1}")).matches(name)
    }
}