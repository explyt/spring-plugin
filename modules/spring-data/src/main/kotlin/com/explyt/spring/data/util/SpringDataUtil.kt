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

package com.explyt.spring.data.util

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.data.SpringDataClasses
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PropertyUtilBase


object SpringDataUtil {
    fun isSpringDataRestProject(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(module, SpringDataClasses.SPRING_DATA_REST_RESOURCE) != null
    }

    fun isSpringDataProject(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(module, SpringDataClasses.REPOSITORY) != null
    }

    fun isSpringDataProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(project, SpringDataClasses.REPOSITORY) != null
    }

    fun isDataModule(module: Module): Boolean {
        return JavaPsiFacade.getInstance(module.project)
            .findClass(SpringDataClasses.REPOSITORY, module.moduleWithLibrariesScope) != null
    }

    fun isSpringDataJpaModule(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(module, SpringDataClasses.JPA_CONTEXT) != null
    }

    fun isRepository(psiClass: PsiClass): Boolean {
        return InheritanceUtil.isInheritor(psiClass, SpringDataClasses.REPOSITORY) ||
                AnnotationUtil.isAnnotated(psiClass, SpringDataClasses.REPOSITORY_ANNOTATION, CHECK_HIERARCHY)
    }

    fun getProperties(domainClass: PsiClass): Set<String> {
        val qualifiedName = domainClass.qualifiedName
        if (qualifiedName == null || qualifiedName.startsWith(JavaCoreClasses.PACKAGE_JAVA_LANG)) return emptySet()
        val all = HashSet<String>()
        all.addAll(PropertyUtilBase.getAllProperties(domainClass, false, true).keys)
        all.addAll(domainClass.fields.map { PropertyUtilBase.suggestPropertyName(it) })
        return all
    }
}