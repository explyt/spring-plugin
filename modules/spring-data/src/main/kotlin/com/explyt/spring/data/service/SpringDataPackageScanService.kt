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

package com.explyt.spring.data.service

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses.COMPONENT
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.PackageScanService.Companion.getBasePackages
import com.explyt.spring.core.service.PackageScanService.Companion.normalizePackage
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.data.SpringDataClasses.ENABLE_JPA_REPOSITORY
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.toUElementOfType

@Service(Service.Level.PROJECT)
class SpringDataPackageScanService(private val project: Project) {

    fun getPackages(module: Module): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                getAllPackagesInner(module),
                ModificationTrackerManager.getInstance(project).getUastAnnotationAndLibraryTracker()
            )
        }
    }

    private fun getAllPackagesInner(module: Module): Set<String> {
        val packages = PackageScanService.getInstance(project).getAllPackages().getPackages(module)

        val annotationPsiClass = getEnableJpaRepositoryAnnotation() ?: return packages
        val annotatedClasses = AnnotatedElementsSearch
            .searchPsiClasses(annotationPsiClass, module.moduleWithDependentsScope)
            .findAll()
            .takeIf { it.isNotEmpty() } ?: return packages
        val annotatedClassesInPackages = annotatedClasses.filter { filterAnnotatedClass(packages, it) }

        val holderComponent = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT)
        val holderEnableJpa = SpringSearchService.getInstance(project).getMetaAnnotations(module, ENABLE_JPA_REPOSITORY)

        val resultPackages = mutableSetOf<String>()
        for (annotatedClass in annotatedClassesInPackages) {
            val uClass = annotatedClass.toUElementOfType<UClass>() ?: continue
            uClass.uAnnotations.find { holderComponent.contains(it) } ?: continue

            resultPackages += uClass.uAnnotations.asSequence()
                .filter { holderEnableJpa.contains(it) }
                .flatMap { getPackages(it, holderEnableJpa) }
                .map { normalizePackage(it) }
        }

        return resultPackages
    }

    private fun filterAnnotatedClass(rootPackages: Set<String>, it: PsiClass?): Boolean {
        val qualifiedName = it?.qualifiedName ?: return false
        return rootPackages.isEmpty() || rootPackages.any { qualifiedName.startsWith(it) }
    }

    private fun getPackages(uAnnotation: UAnnotation, metaAnnotationsHolder: MetaAnnotationsHolder): Set<String> {
        val qualifiedName = uAnnotation.qualifiedName ?: return emptySet()
        val basePackagesValue = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName,
                    it.name ?: SpringProperties.VALUE,
                    ENABLE_JPA_REPOSITORY,
                    setOf(SpringProperties.VALUE)
                )
            }
            .flatMap { getBasePackages(it.expression) }
            .toSet()
        val basePackages = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, ENABLE_JPA_REPOSITORY, setOf("basePackages")
                )
            }
            .flatMap { getBasePackages(it.expression) }
            .toSet()
        val basePackageClasses = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, ENABLE_JPA_REPOSITORY, setOf("basePackageClasses")
                )
            }
            .flatMap { PackageScanService.getClassPackages(it.expression) }
            .toSet()
        val packages = basePackagesValue + basePackages + basePackageClasses
        //case with empty params. example: @ComponentScan
        if (packages.isEmpty()) {
            val packageName = uAnnotation.getContainingUFile()?.packageName ?: return emptySet()
            return setOf(packageName)
        }
        return packages
    }

    private fun getEnableJpaRepositoryAnnotation(): PsiClass? {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                LibraryClassCache.searchForLibraryClass(project, ENABLE_JPA_REPOSITORY),
                ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    companion object {
        fun getInstance(project: Project): SpringDataPackageScanService = project.service()
    }
}
