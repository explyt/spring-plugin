package com.explyt.spring.core.profile

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.MethodReferencesSearch
import org.jetbrains.uast.*

class AdditionalProfileSearcher(private val project: Project) : ProfileSearcher {

    override fun searchActiveProfiles(module: Module): List<String> {
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

    override fun searchProfiles(module: Module): List<String> {
        return searchActiveProfiles(module)
    }

    companion object {
        const val SET_ADDITIONAL_PROFILES = "setAdditionalProfiles"
    }
}