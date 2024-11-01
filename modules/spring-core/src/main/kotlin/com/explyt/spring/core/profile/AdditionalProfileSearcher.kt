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