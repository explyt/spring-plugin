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

package com.explyt.spring.core.service

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses.COMPONENT
import com.explyt.spring.core.SpringCoreClasses.COMPONENT_SCAN
import com.explyt.spring.core.SpringCoreClasses.COMPONENT_SCANS
import com.explyt.spring.core.SpringCoreClasses.IMPORT
import com.explyt.spring.core.SpringCoreClasses.SPRING_BOOT_APPLICATION
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.runconfiguration.RunConfigurationUtil
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.execution.RunManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.*

@Service(Service.Level.PROJECT)
class PackageScanService(private val project: Project) {

    fun getAllPackages(): RootDataHolder {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAllPackagesInner(),
                ModificationTrackerManager.getInstance(project).getUastAnnotationAndLibraryTracker()
            )
        }
    }

    private fun getAllPackagesInner(): RootDataHolder {
        val annotationPsiClasses = getComponentScanAnnotations()
        val rootClasses = searchRootClasses(annotationPsiClasses)
        val rootModuleData = rootClasses.mapNotNull { findRootModulePackages(it) }
        val programConfigRootClasses = AnnotationConfigApplicationService.getRootClasses(rootClasses, rootModuleData)

        val rootPackages = rootModuleData.flatMapTo(mutableSetOf()) { it.packages }
        val scanModuleData = getComponentScanClasses(annotationPsiClasses, rootPackages, programConfigRootClasses)

        val moduleRootDataList = rootModuleData + scanModuleData

        val importAnnotationClass = annotationPsiClasses.importAnnotationClass
        val importClasses = getImportClasses(moduleRootDataList, importAnnotationClass, programConfigRootClasses)
        val importModuleRootDataList = importClasses.mapNotNull { toModulePackages(it, true) }
        val allModuleRootDataList = moduleRootDataList + importModuleRootDataList

        val packagesByModuleName = allModuleRootDataList.groupingBy { it.moduleName }
            .fold(setOf<String>()) { acc, ell -> acc + ell.packages.map { normalizePackage(it) } }
        val rootComponentQualified = moduleRootDataList.mapNotNullTo(mutableSetOf()) { it.rootComponentQualified }
        val importQualified = importClasses.mapNotNullTo(mutableSetOf()) { it.qualifiedName }

        return RootDataHolder(packagesByModuleName, rootComponentQualified, importQualified)
    }

    private fun getComponentScanClasses(
        annotationPsiClasses: ScanAnnotationHolder, rootPackages: MutableSet<String>, programConfigRoots: List<PsiClass>
    ): List<ModuleRootData> {
        val classesForSearchComponentScan = programConfigRoots.ifEmpty {
            annotationPsiClasses.scanAnnotationClass
                .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
        }
        return classesForSearchComponentScan
            .filter { filterComponentScanClass(rootPackages, it) }
            .mapNotNull { toModulePackages(it) }
    }

    private fun getImportClasses(
        moduleRootData: List<ModuleRootData>, importAnnotationClass: Set<PsiClass>, programConfigRoots: List<PsiClass>
    ): Set<PsiClass> {
        val packages = moduleRootData.flatMapTo(mutableSetOf()) { it.packages }
        val classesForSearchImport = programConfigRoots.ifEmpty {
            importAnnotationClass
                .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
        }
        return classesForSearchImport
            .filter { filterComponentScanClass(packages, it) }
            .flatMapTo(mutableSetOf()) { getImportClasses(it) }
    }

    private fun searchRootClasses(annotationPsiClasses: ScanAnnotationHolder): List<PsiClass> {
        if (Registry.`is`("explyt.spring.root.runConfiguration")) {
            val runConfiguration = RunManager.getInstance(project).selectedConfiguration
                ?.configuration as? SpringBootRunConfiguration
            if (runConfiguration != null) {
                return RunConfigurationUtil.getRunPsiClass(runConfiguration)
            }
        }

        return annotationPsiClasses.rootAnnotationClass
                .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
    }

    private fun filterComponentScanClass(rootPackages: Set<String>, it: PsiClass?): Boolean {
        val packageName = (it?.containingFile as? PsiJavaFile)?.packageName ?: return false
        return rootPackages.isEmpty() || rootPackages.any { packageName.startsWith(it) }
    }

    private fun findRootModulePackages(psiClass: PsiClass): ModuleRootData? {
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null
        val uClass = psiClass.toUElementOfType<UClass>() ?: return null

        val holderSpringBoot = SpringSearchService.getInstance(project)
            .getMetaAnnotations(module, SPRING_BOOT_APPLICATION)

        val springBootAnnotations = uClass.uAnnotations.filter { holderSpringBoot.contains(it) }
        if (springBootAnnotations.isEmpty()) return null

        val holderScan = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT_SCAN)
        val bootPackages = springBootAnnotations.flatMapTo(mutableSetOf()) { getPackages(it, holderScan) }

        val scanPackages = uClass.uAnnotations.asSequence()
            .filter { !springBootAnnotations.contains(it) }
            .filter { holderScan.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackages(it, holderScan) }

        //annotation @ComponentScan override @SpringBootApplication on some class
        val packages = scanPackages.ifEmpty { bootPackages }
        return ModuleRootData(module.name, packages, psiClass.qualifiedName)
    }

    private fun toModulePackages(psiClass: PsiClass, isImport: Boolean = false): ModuleRootData? {
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null
        val uClass = psiClass.toUElementOfType<UClass>() ?: return null

        if (!isImport) {
            val holderComponent = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT)
            uClass.uAnnotations.find { holderComponent.contains(it) } ?: return null
        }

        val holderScan = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT_SCAN)
        val packages = uClass.uAnnotations.asSequence()
            .filter { holderScan.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackages(it, holderScan) }

        val holderScans = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT_SCANS)
        val packagesScans = uClass.uAnnotations.asSequence()
            .filter { holderScans.contains(it) }
            .flatMapTo(mutableSetOf()) { getPackagesScans(it, holderScan) }

        val allPackages = packages + packagesScans
        if (allPackages.isEmpty()) return null
        return ModuleRootData(module.name, allPackages)
    }

    private fun getImportClasses(psiClass: PsiClass): Set<PsiClass> {
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return emptySet()
        val uClass = psiClass.toUElementOfType<UClass>() ?: return emptySet()

        val holderComponent = SpringSearchService.getInstance(project).getMetaAnnotations(module, COMPONENT)
        uClass.uAnnotations.find { holderComponent.contains(it) } ?: return emptySet()

        val holderImport = SpringSearchService.getInstance(project).getMetaAnnotations(module, IMPORT)
        return uClass.uAnnotations.asSequence()
            .filter { holderImport.contains(it) }
            .flatMapTo(mutableSetOf()) { getImportClasses(it, holderImport) }
    }

    private fun getPackagesScans(uAnnotation: UAnnotation, metaAnnotationsHolder: MetaAnnotationsHolder): Set<String> {
        val findAttributeValue = uAnnotation.findAttributeValue(SpringProperties.VALUE) ?: return emptySet()
        val memberValue = findAttributeValue.javaPsi as? PsiAnnotationMemberValue ?: return emptySet()
        return AnnotationUtil.arrayAttributeValues(memberValue)
            .mapNotNull { it.toUElement() as? UAnnotation }
            .flatMapTo(mutableSetOf()) { getPackages(it, metaAnnotationsHolder) }
    }

    private fun getImportClasses(
        uAnnotation: UAnnotation,
        metaAnnotationsHolder: MetaAnnotationsHolder
    ): Set<PsiClass> {
        val qualifiedName = uAnnotation.qualifiedName ?: return emptySet()
        return uAnnotation.attributeValues.asSequence()
            .filter {
                metaAnnotationsHolder.isAttributeRelatedWith(
                    qualifiedName, it.name ?: SpringProperties.VALUE, IMPORT, setOf(SpringProperties.VALUE)
                )
            }
            .flatMap { getPsiClasses(it.expression) }
            .toSet()
    }

    private fun getComponentScanAnnotations(): ScanAnnotationHolder {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getComponentScanAnnotationsInner(),
                ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    private fun getComponentScanAnnotationsInner(): ScanAnnotationHolder {
        val componentScanClass = LibraryClassCache.searchForLibraryClass(project, COMPONENT_SCAN)
            ?: return ScanAnnotationHolder()
        val componentScansClass = LibraryClassCache.searchForLibraryClass(project, COMPONENT_SCANS)
            ?: return ScanAnnotationHolder()
        val importClass = LibraryClassCache.searchForLibraryClass(project, IMPORT)
            ?: return ScanAnnotationHolder()
        val childrenScan = MetaAnnotationUtil.getChildren(componentScanClass, GlobalSearchScope.allScope(project))
        val childrenImport = MetaAnnotationUtil.getChildren(importClass, GlobalSearchScope.allScope(project))

        val rootAnnotationClass = getSpringBootAppAnnotations()
        val scanAnnotationClass = (childrenScan + componentScanClass + componentScansClass)
            .filterTo(mutableSetOf()) { !rootAnnotationClass.contains(it) }
        val importClasses = (childrenImport + importClass).toSet()

        return ScanAnnotationHolder(rootAnnotationClass, scanAnnotationClass, importClasses)
    }

    private fun getSpringBootAppAnnotations(): Set<PsiClass> {
        val springBootAppClass = LibraryClassCache.searchForLibraryClass(project, SPRING_BOOT_APPLICATION)
            ?: return emptySet()
        val childrenBoot = MetaAnnotationUtil.getChildren(springBootAppClass, GlobalSearchScope.allScope(project))
        return (childrenBoot + springBootAppClass).toSet()
    }

    companion object {
        fun getInstance(project: Project): PackageScanService = project.service()

        fun normalizePackage(packageName: String): String {
            if (packageName.endsWith(".")) return packageName
            if (packageName.endsWith("*")) return normalizePackage(packageName.substring(0, packageName.length - 2))
            return "$packageName."
        }

        fun getPackages(uAnnotation: UAnnotation, metaAnnotationsHolder: MetaAnnotationsHolder): Set<String> {
            val qualifiedName = uAnnotation.qualifiedName ?: return emptySet()
            val rootMetaClass = metaAnnotationsHolder.getRootClassQualified()
            val basePackages = uAnnotation.attributeValues.asSequence()
                .filter {
                    metaAnnotationsHolder.isAttributeRelatedWith(
                        qualifiedName, it.name ?: SpringProperties.VALUE, rootMetaClass, setOf(SpringProperties.VALUE)
                    )
                }
                .flatMap { getBasePackages(it.expression) }
                .toSet()
            val basePackageClasses = uAnnotation.attributeValues.asSequence()
                .filter {
                    metaAnnotationsHolder.isAttributeRelatedWith(
                        qualifiedName, it.name ?: SpringProperties.VALUE, rootMetaClass, setOf("basePackageClasses")
                    )
                }
                .flatMap { getClassPackages(it.expression) }
                .toSet()
            val packages = basePackages + basePackageClasses
            //case with empty params. example: @ComponentScan
            if (packages.isEmpty()) {
                val packageName = uAnnotation.getContainingUFile()?.packageName ?: return emptySet()
                return setOf(packageName)
            }
            return packages
        }

        fun getBasePackages(uExpression: UExpression): List<String> {
            return if (uExpression is UCallExpression) {
                uExpression.valueArguments.mapNotNull { it.evaluate() as? String }
            } else {
                (uExpression.evaluate() as? String)?.let { listOf(it) } ?: emptyList()
            }
        }

        fun getClassPackages(uExpression: UExpression): List<String> {
            return getPsiClasses(uExpression).mapNotNull { (it.containingFile as? PsiJavaFile)?.packageName }
        }

        fun getPsiClasses(uExpression: UExpression): List<PsiClass> {
            return if (uExpression is UCallExpression) {
                uExpression.valueArguments.mapNotNull { getPsiClass(it) }
            } else {
                getPsiClass(uExpression)?.let { listOf(it) } ?: emptyList()
            }
        }

        private fun getPsiClass(uExpression: UExpression): PsiClass? {
            val uClassLiteralExpression = uExpression as? UClassLiteralExpression ?: return null
            return uClassLiteralExpression.type?.resolvedPsiClass
        }
    }

    data class ModuleRootData(
        val moduleName: String, val packages: Set<String>, val rootComponentQualified: String? = null
    )

    private data class ScanAnnotationHolder(
        val rootAnnotationClass: Set<PsiClass> = emptySet(),
        val scanAnnotationClass: Set<PsiClass> = emptySet(),
        val importAnnotationClass: Set<PsiClass> = emptySet()
    )
}

data class RootDataHolder(
    private val packagesByModuleName: Map<String, Set<String>>,
    val rootComponentQualified: Set<String>,
    val importQualified: Set<String>
) {
    fun isEmpty() = packagesByModuleName.isEmpty()

    fun isRootComponent(qualifiedName: String) = rootComponentQualified.contains(qualifiedName)

    fun getPackages(module: Module): Set<String> {
        val packages = packagesByModuleName.getOrDefault(module.name, emptySet())
        val dependentModules = ModuleManager.getInstance(module.project).getModuleDependentModules(module)
        val dependentPackages = dependentModules
            .flatMapTo(mutableSetOf()) { packagesByModuleName.getOrDefault(it.name, emptySet()) }
        val resultPackages = dependentPackages + packages
        if (resultPackages.isEmpty() && module.name.endsWith(".test")) {
            val mainModuleName = module.name.substringBeforeLast(".test") + ".main"
            val mainModule = ModuleManager.getInstance(module.project)
                .findModuleByName(mainModuleName) ?: return emptySet()
            return getPackages(mainModule)
        }
        return resultPackages
    }
}