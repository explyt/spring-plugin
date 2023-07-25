package com.esprito.spring.core.profile

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreClasses
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.MethodReferencesSearch
import org.jetbrains.uast.*

class AdditionalProfileSearcher(
    private val project: Project
) : ProfileSearcher {
    override fun searchProfiles(module: Module): List<String> {
        val springApplication = LibraryClassCache.searchForLibraryClass(
            project,
            SpringCoreClasses.SPRING_APPLICATION
        ) ?: return emptyList()

        val psiMethod = springApplication.methods.firstOrNull {
            it.name == SET_ADDITIONAL_PROFILES
        } ?: return emptyList()

        return MethodReferencesSearch
            .search(psiMethod, module.moduleScope, true)
            .asSequence()
            .mapNotNull {
                it.element.toUElementOfType<UReferenceExpression>()
                    ?.getParentOfType<UCallExpression>()
            }.filter {
                it.methodName == SET_ADDITIONAL_PROFILES
            }.flatMap {
                it.valueArguments
            }.mapNotNull {
                it.evaluateString()
            }.toList()
    }

    companion object {
        const val SET_ADDITIONAL_PROFILES = "setAdditionalProfiles"
    }
}