/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties.utils

import com.explyt.module.ExternalSystemModule
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.*
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.base.util.projectScope


object ProjectConfigurationPropertiesUtil {

    fun getAnnotatedElements(module: Module): Iterable<PsiModifierListOwner> {
        val librariesSearchScope = ExternalSystemModule.of(module).librariesSearchScope
        val configurationPropertiesClass = JavaPsiFacade.getInstance(module.project)
            .findClass(SpringCoreClasses.CONFIGURATION_PROPERTIES, librariesSearchScope)
            ?: return emptyList()

        val resultElements = AnnotatedElementsSearch.searchElements(
            configurationPropertiesClass,
            module.moduleScope,
            PsiClass::class.java, PsiMethod::class.java
        ).toMutableSet()

        val moduleRootManager = ModuleRootManager.getInstance(module)
        for (dependentModule in moduleRootManager.dependencies) {
            resultElements += AnnotatedElementsSearch.searchElements(
                configurationPropertiesClass,
                dependentModule.moduleScope,
                PsiClass::class.java, PsiMethod::class.java
            )
        }

        return resultElements
    }

    fun getAnnotatedElements(project: Project): Iterable<PsiModifierListOwner> {
        val configurationPropertiesClass = JavaPsiFacade.getInstance(project)
            .findClass(SpringCoreClasses.CONFIGURATION_PROPERTIES, project.allScope())
            ?: return emptyList()

        val resultElements = AnnotatedElementsSearch.searchElements(
            configurationPropertiesClass,
            project.projectScope(),
            PsiClass::class.java, PsiMethod::class.java
        ).toMutableSet()

        return resultElements
    }

    fun extractConfigurationPropertyPrefix(module: Module, annotatedElement: PsiModifierListOwner): String? {
        if (annotatedElement !is PsiMember) {
            return null
        }
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONFIGURATION_PROPERTIES)
        val prefix = metaHolder.getAnnotationMemberValues(annotatedElement, setOf("value")).firstOrNull() ?: return null
        return AnnotationUtil.getStringAttributeValue(prefix)
    }

    fun extractConfigurationPropertyPrefix(module: Module, annotatedElement: PsiAnnotation): String? {
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONFIGURATION_PROPERTIES)
        val prefix = metaHolder.getAnnotationMemberValues(annotatedElement, setOf("value")).firstOrNull() ?: return null
        return AnnotationUtil.getStringAttributeValue(prefix)
    }

}
