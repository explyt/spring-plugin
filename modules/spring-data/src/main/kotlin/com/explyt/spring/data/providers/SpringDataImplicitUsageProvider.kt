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

package com.explyt.spring.data.providers

import com.explyt.spring.data.SpringDataClasses.SPRING_DATA_REST_RESOURCE
import com.explyt.spring.data.util.SpringDataUtil
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isNonStatic
import com.explyt.util.ExplytPsiUtil.isOrdinaryClass
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod


class SpringDataImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        return when (element) {
            is PsiClass -> {
                if (element.isInterface) {
                    return element.isAnnotatedBy(SPRING_DATA_REST_RESOURCE) || isMetaAnnotatedRestResource(element)
                }
                if (element.isOrdinaryClass && element.isPossibleCustomRepository()) {
                    val module = ModuleUtilCore.findModuleForPsiElement(element)
                    return module != null && SpringDataUtil.isSpringDataProject(module)
                }
                return element.isAnnotatedBy(IMPLICIT_CLASS_ANNOTATIONS)
            }

            is PsiMethod -> {
                if (element.isConstructor) {
                    return element.isAnnotatedBy(IMPLICIT_CONSTRUCTOR_ANNOTATIONS)
                }
                return element.isNonStatic && element.isAnnotatedBy(IMPLICIT_METHOD_ANNOTATIONS)
            }

            is PsiField -> {
                element.isAnnotatedBy(IMPLICIT_FIELD_ANNOTATIONS)
            }

            else -> {
                false
            }
        }
    }

    override fun isImplicitRead(element: PsiElement): Boolean {
        if (element is PsiField) {
            element.containingClass
            return element.isAnnotatedBy(IMPLICIT_FIELD_ANNOTATIONS)
        }
        return false
    }

    override fun isImplicitWrite(element: PsiElement): Boolean {
        if (element is PsiField) {
            return element.isAnnotatedBy(IMPLICIT_FIELD_ANNOTATIONS)
        }
        return false
    }

    private fun PsiClass.isPossibleCustomRepository(): Boolean {
        val suspectClassName: String = this.name ?: ""
        if (suspectClassName.endsWith("RepositoryImpl")) {
            val repositoryInterfaceName = suspectClassName.removeSuffix("Impl")
            return this.supers.any {
                it.isInterface && it.name == repositoryInterfaceName
            }
        }
        return false
    }

    private fun isMetaAnnotatedRestResource(element: PsiClass): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false
        if (!SpringDataUtil.isSpringDataRestProject(module)) {
            return false
        }
        val restEndpointAnnotations = MetaAnnotationUtil
            .getAnnotationTypesWithChildren(module, SPRING_DATA_REST_RESOURCE, false)
            .mapNotNull { it.qualifiedName }
        return element.isAnnotatedBy(restEndpointAnnotations)
    }

    companion object {
        private val IMPLICIT_CLASS_ANNOTATIONS: List<String> = listOf(
            "org.springframework.data.rest.core.annotation.HandleAfterCreate",
            "org.springframework.data.rest.core.annotation.HandleAfterDelete",
            "org.springframework.data.rest.core.annotation.RepositoryEventHandler",
        )
        private val IMPLICIT_FIELD_ANNOTATIONS: List<String> = listOf(
            "org.springframework.data.annotation.Id",
            "org.springframework.data.annotation.Version",
        )
        private val IMPLICIT_METHOD_ANNOTATIONS: List<String> = listOf(
            "org.springframework.data.rest.core.annotation.HandleAfterCreate",
            "org.springframework.data.rest.core.annotation.HandleAfterDelete",
        )
        private val IMPLICIT_CONSTRUCTOR_ANNOTATIONS: List<String> = listOf(
            "org.springframework.data.annotation.PersistenceConstructor"
        )
    }
}