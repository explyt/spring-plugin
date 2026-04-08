/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses.COMPONENT
import com.explyt.spring.core.SpringCoreClasses.ENTITY_SCAN
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.service.PackageScanService.Companion.getBasePackages
import com.explyt.spring.core.service.PackageScanService.Companion.normalizePackage
import com.explyt.spring.core.tracker.ModificationTrackerManager
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
class SpringEntityPackageScanService(private val project: Project) {

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

        val annotationPsiClass = getEntityScanAnnotation() ?: return packages
        val annotatedClasses = AnnotatedElementsSearch
            .searchPsiClasses(annotationPsiClass, module.moduleWithDependentsScope)
            .findAll()
            .takeIf { it.isNotEmpty() } ?: return packages
        val annotatedClassesInPackages = annotatedClasses.filter { filterAnnotatedClass(packages, it) }

        val holderComponent = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT)
        val holderEntityScan = SpringSearchService.getInstance(project).getMetaAnnotations(module, ENTITY_SCAN)

        val resultPackages = mutableSetOf<String>().also { it.addAll(packages) }
        for (annotatedClass in annotatedClassesInPackages) {
            val uClass = annotatedClass.toUElementOfType<UClass>() ?: continue
            uClass.uAnnotations.find { holderComponent.contains(it) } ?: continue

            resultPackages += uClass.uAnnotations.asSequence()
                .filter { holderEntityScan.contains(it) }
                .flatMap { getPackages(it, holderEntityScan) }
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
        val basePackages = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, ENTITY_SCAN, setOf(SpringProperties.VALUE)
                )
            }
            .flatMap { getBasePackages(it.expression) }
            .toSet()

        val basePackageClasses = uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, ENTITY_SCAN, setOf("basePackageClasses")
                )
            }
            .flatMap { PackageScanService.getClassPackages(it.expression) }
            .toSet()
        val packages = basePackages + basePackageClasses
        //case with empty params.
        if (packages.isEmpty()) {
            val packageName = uAnnotation.getContainingUFile()?.packageName ?: return emptySet()
            return setOf(packageName)
        }
        return packages
    }

    private fun getEntityScanAnnotation(): PsiClass? {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                LibraryClassCache.searchForLibraryClass(project, ENTITY_SCAN),
                ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    companion object {
        fun getInstance(project: Project): SpringEntityPackageScanService = project.service()
    }
}
