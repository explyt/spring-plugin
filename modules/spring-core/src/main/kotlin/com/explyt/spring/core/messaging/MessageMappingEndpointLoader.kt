/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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