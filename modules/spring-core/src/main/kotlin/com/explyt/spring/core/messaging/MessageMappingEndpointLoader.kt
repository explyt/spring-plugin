/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.core.messaging

import com.explyt.spring.core.SpringCoreClasses
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

/**
 * todo refactor:
 * - MessageBrokerEndpointLoader to spring-messaging module
 * - SpringWebEndpointsLoader rename to SpringEndpointsLoader and to core module
 * */
class MessageMappingEndpointLoader {
    companion object {

        fun searchMessageMappingClasses(module: Module, scope: GlobalSearchScope): Collection<PsiClass> {
            return searchMessageMappingMethods(module, scope).asSequence().mapNotNull { it.containingClass }.toSet()
        }

        fun searchMessageMappingMethods(module: Module, scope: GlobalSearchScope? = null): List<PsiMethod> {
            val springMessageAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
                module, SpringCoreClasses.MESSAGE_MAPPING, false
            ).takeIf { it.isNotEmpty() } ?: emptyList()

            val eeMessageAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
                module, SpringCoreClasses.MESSAGE_MAPPING_EE, false
            ).takeIf { it.isNotEmpty() } ?: emptyList()

            val allAnnotations = springMessageAnnotations + eeMessageAnnotations

            return allAnnotations.asSequence()
                .flatMap { AnnotatedElementsSearch.searchPsiMethods(it, scope ?: module.moduleScope) }
                .toList()
        }
    }
}