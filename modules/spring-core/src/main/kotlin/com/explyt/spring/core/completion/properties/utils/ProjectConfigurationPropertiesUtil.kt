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

package com.explyt.spring.core.completion.properties.utils

import com.explyt.module.ExternalSystemModule
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.*
import com.intellij.psi.search.searches.AnnotatedElementsSearch


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
